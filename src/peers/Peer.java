package peers;

import chord.ChordInfo;
import chord.ChordNode;
import communication.SendMessageThread;
import communication.protocols.BackupMessage;
import communication.protocols.DeleteMessage;
import communication.protocols.RestoreMessage;
import files.FileC;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;


public class Peer extends ChordNode implements RMIRemoteObject {
    private static InetAddress socketAddress;
    private static int port;
    private static Storage storage;
    public static ExecutorService exec = Executors.newFixedThreadPool(256);

    public Peer(InetAddress bootPeerAddress, int bootPeerPort) throws IOException, ClassNotFoundException {
        super(new InetSocketAddress(socketAddress, port), new InetSocketAddress(bootPeerAddress, bootPeerPort));
        storage = new Storage(ChordNode.getChordID());
    }

    public static void main(String[] args) throws IOException {
        boolean msg = false;
        if (args.length != 6) {
            System.out.println("Usage:\tPeer <ProtocolVersion> <PeerAP> <PeerIP> <PeerPort> <BootPeerIP> <BootPeerPort>\n");
            return;
        }

        String protocol_version = args[0];
        String rmt_obj_name = args[1];
        socketAddress = InetAddress.getByName(args[2]);
        port = Integer.parseInt(args[3]);
        InetAddress bootPeerAddress = InetAddress.getByName(args[4]);
        int bootPeerPort = Integer.parseInt(args[5]);

        try {
            Peer peer = new Peer(bootPeerAddress, bootPeerPort);
            RMIRemoteObject stub = (RMIRemoteObject) UnicastRemoteObject.exportObject((Remote) peer, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(rmt_obj_name, stub);
            new Thread(peer::start).start();
        } catch (Exception e) {
            System.err.println("Peer exception: " + e.toString());
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Peer::leave));
    }

    public static void leave() {
        long spaceUsed = storage.getSpaceAvailable() - storage.getSpaceOccupied(); // Space used before reclaim
        if (0 >= spaceUsed) { // Doesn't make sense to reclaim on a peer that already meets the requirements
            System.out.println("Peer already meets the requirements! No need to delete any chunks.");
            Peer.getStorage().setCapacity(0);
            Peer.getStorage().setSpaceAvailable(0 - Peer.getStorage().getSpaceOccupied());
        } else {
            List<FileC> files = Peer.getStorage().getStoredFiles();
            List<FileC> filesToRemove = new ArrayList<>();
            files.sort(Comparator.comparing(FileC::sortFiles));
            Collections.reverse(files);

            for (FileC fileToRemove: files) {
                if (0 >= spaceUsed) // When the requirements are met, there is no need for more deletion
                    break;
                else {
                    spaceUsed -= fileToRemove.getSize();
                    filesToRemove.add(fileToRemove);

                    fileToRemove.setRepDegree(1);
                    fileToRemove.clearPeers();
                    fileToRemove.setParentPeer(Peer.self);
                    BackupMessage msg = new BackupMessage(ChordNode.peerAddress, fileToRemove);

                    exec.execute(new SendMessageThread(msg, null, ChordNode.getSuccessor().getSocketAddress()));
                }
            }

            // After we know which files ought to be deleted, we delete them
            for (FileC f: filesToRemove) {
                storage.removeFile(f);
            }

            Peer.getStorage().setCapacity(0);
            Peer.getStorage().setSpaceAvailable(0);
        }
        ChordNode.leave();
    }

    public static Storage getStorage() {
        return storage;
    }

    public static ExecutorService getExec() {
        return exec;
    }

    public void start() {
        try {
            this.join();
            scheduler.scheduleAtFixedRate(this::stabilize, 3, 2, TimeUnit.SECONDS);
            scheduler.scheduleAtFixedRate(this::fixFingers, 10, 3, TimeUnit.SECONDS);
            scheduler.scheduleAtFixedRate(this::checkPredecessor, 10, 10, TimeUnit.SECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void backup(String path, int repDegree) throws IOException, InterruptedException {
        if (storage.checkIfAlreadyBackedUp(path)) {
            System.out.println("This file has already been backed up!");
            return;
        }
        FileC file = new FileC(path, repDegree);
        file.setParentPeer(ChordNode.self);
        file.setOriginalPeer(ChordNode.self);
        FileInputStream fileInput = new FileInputStream(file.getPath());
        byte[] body = fileInput.readAllBytes();
        file.setBody(body);

        BackupMessage msg = new BackupMessage(ChordNode.peerAddress, file);

        exec.execute(new SendMessageThread(msg, null, ChordNode.getSuccessor().getSocketAddress()));
    }

    @Override
    public void state() throws IOException {
        System.out.println("\n-------------------------------------------------------------------");
        System.out.println("PEER " + Peer.getChordID() + " STATE");
        System.out.println("-------------------------------------------------------------------");
        System.out.println("Backed Up Files\n");

        if (storage.getBackedFiles().isEmpty()) {
            System.out.println("No Backed Up Files Yet");
            System.out.println("-------------------------------------------------------------------");
        }

        int cnt = 0;
        List<FileC> aux = new ArrayList<>();
        for (FileC file: storage.getBackedFiles()){
            if(file.getRepDegree() == 0) aux.add(file);
            else{
                cnt++;
                System.out.println("File " + cnt);
                System.out.println("Pathname: " + file.getPath());
                System.out.println("Backup ID: " + file.getFile_id());
                System.out.println("Desired Replication Degree: " + file.getRepDegree());
                System.out.println("Perceived Replication Degree: " + file.getCurrentRepDegree());
                System.out.println("Peers with this File: ");
                for (ChordInfo chord_info: file.getPeersWithBackup()) {
                    System.out.println("  - " + chord_info.getChordID());
                }
                System.out.println("\n");
            }
        }
        if(aux.isEmpty()){
            System.out.println("-------------------------------------------------------------------");
            System.out.println("All requested file deletions were successful!");
            System.out.println("-------------------------------------------------------------------\n");
        }
        else{
            System.out.println("-------------------------------------------------------------------");
            System.out.println("Unsuccessful file deletions request:\n" );
            cnt = 0;
            for(FileC file : aux) {
                cnt++;
                System.out.println("File " + cnt);
                System.out.println("Pathname: " + file.getPath());
                System.out.println("Backup ID: " + file.getFile_id());
                System.out.println("Perceived Replication Degree: " + file.getCurrentRepDegree());
                System.out.println("Peers with this File: ");
                for (ChordInfo chord_info : file.getPeersWithBackup()) {
                    System.out.println("  - " + chord_info.getChordID());
                }
                System.out.println("\n");
            }
            System.out.println("-------------------------------------------------------------------\n");
        }

        /*if(Peer.getProtocol_version().equals("1.9")){
            System.out.println("Deleted files: ");
            if (storage.getDeletedFiles().isEmpty())
                System.out.println("No Deleted Files Queue");
            for (FileC file: storage.getDeletedFiles()) {
                System.out.println("Backup ID: " + file.getFile_id());
                System.out.println("Number of Chunks to Remove: " + file.getRepDegree() + "\n");
            }
        }*/
        System.out.println("Stored Files\n");
        if (storage.getStoredFiles().isEmpty()) {
            System.out.println("No Stored Up Files Yet");
            System.out.println("-------------------------------------------------------------------");
        }

        cnt = 0;
        for (FileC file: storage.getStoredFiles()){
            cnt++;
            System.out.println("File " + cnt);
            System.out.println("Pathname: " + file.getPath());
            System.out.println("Backup ID: " + file.getFile_id());
            System.out.println("Desired Replication Degree: " + file.getRepDegree());
            System.out.println("\n");
        }
        System.out.println("Storage Capacity: " + storage.getCapacity());
        System.out.println("Available Space: " + storage.getSpaceAvailable());
        System.out.println("Ocuppied Space: " + storage.getSpaceOccupied());
        System.out.println("-------------------------------------------------------------------");

    }

    @Override
    public void restore(String path) throws RemoteException, InterruptedException {
        boolean fileFound = false; // Used for checking if file was backed up already
        for (FileC file: storage.getBackedFiles()){
            if (file.getFile().getName().equals(path) || file.getPath().equals(path) || file.getFile().getAbsolutePath().equals((path))) {
                if(fileFound) {
                    System.out.println("Ambiguous file name, restoring another file with the same filename.\n");
                }
                fileFound = true;
                for (ChordInfo c: file.getPeersWithBackup()) {
                    RestoreMessage msg = new RestoreMessage(ChordNode.peerAddress, file);
                    exec.execute(new SendMessageThread(msg, null, c.getSocketAddress()));
                }
            }

        }
        if (!fileFound) // If the file is not in the backed files list, it doesn't make sense to restore it
            System.out.println("File hasn't been backed up yet!");
    }

    @Override
    public void delete(String path) throws IOException, InterruptedException {
        boolean fileFound = false;
        for(FileC file : storage.getBackedFiles()){
            if(file.getFile().getName().equals(path) || file.getFile().getAbsolutePath().equals(path) || file.getPath().equals(path)){
                file.setRepDegree(0);
                //System.out.println(file.getRepDegree());
                fileFound = true;
                for(ChordInfo chordInfo : file.getPeersWithBackup()){
                    DeleteMessage deleteMessage = new DeleteMessage(ChordNode.peerAddress, file);
                    exec.execute(new SendMessageThread(deleteMessage, null, chordInfo.getSocketAddress()));
                }

            }
        }
        if(!fileFound)
            System.out.println("File not found!");
    }

    @Override
    public void reclaim(long space) throws IOException, InterruptedException {
        long spaceUsed = storage.getSpaceAvailable() - storage.getSpaceOccupied(); // Space used before reclaim
        if (space >= spaceUsed) { // Doesn't make sense to reclaim on a peer that already meets the requirements
            System.out.println("\n-------------------------------------------------------------------");
            System.out.println("PEER ALREADY MEETS THE REQUIREMENTS!");
            System.out.println("-------------------------------------------------------------------");
            Peer.getStorage().setCapacity(space);
            Peer.getStorage().setSpaceAvailable(space - Peer.getStorage().getSpaceOccupied());
        } else {
            List<FileC> files = Peer.getStorage().getStoredFiles();
            List<FileC> filesToRemove = new ArrayList<>();
            files.sort(Comparator.comparing(FileC::sortFiles));
            Collections.reverse(files);

            for (FileC fileToRemove: files) {
                if (space >= spaceUsed) // When the requirements are met, there is no need for more deletion
                    break;
                else {
                    spaceUsed -= fileToRemove.getSize();
                    filesToRemove.add(fileToRemove);

                    fileToRemove.setRepDegree(1);
                    fileToRemove.clearPeers();
                    fileToRemove.setParentPeer(Peer.self);
                    BackupMessage msg = new BackupMessage(ChordNode.peerAddress, fileToRemove);

                    exec.execute(new SendMessageThread(msg, null, ChordNode.getSuccessor().getSocketAddress()));
                }
            }

            // After we know which files ought to be deleted, we delete them
            for (FileC f: filesToRemove) {
                storage.removeFile(f);
            }

            Peer.getStorage().setCapacity(space);
            Peer.getStorage().setSpaceAvailable(space - Peer.getStorage().getSpaceOccupied());
            System.out.println("\n-------------------------------------------------------------------");
            System.out.println("NEW SPACE AVAILABLE: " + Peer.getStorage().getSpaceAvailable());
            System.out.println("-------------------------------------------------------------------");

        }
    }
}