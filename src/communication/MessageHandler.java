package communication;

import chord.ChordInfo;
import chord.ChordNode;
import communication.protocols.*;
import files.FileC;
import peers.Peer;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class MessageHandler implements Runnable {
    private final ProtocolMessage received;
    private final SSLSocket clientSocket;

    public MessageHandler(ProtocolMessage received, SSLSocket clientSocket) {
        this.received = received;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        if (received instanceof BackupMessage) {
            this.handleBackup();
        }
        else if (received instanceof StoredMessage) {
            this.handleStored();
        }
        else if (received instanceof RestoreMessage) {
            this.handleRestore();
        }
        else if (received instanceof UpdateFileMessage) {
            this.handleUpdateFile();
        }
        else if(received instanceof DeleteMessage){
            this.handleDelete();
        }
        else if(received instanceof DeletedMessage){
            this.handleDeleted();
        }
    }

    private void handleDeleted() {
        FileC file = received.getFile();

        for(int i = 0; i < Peer.getStorage().getBackedFiles().size(); i++){
            if(Peer.getStorage().getBackedFiles().get(i).equals(file)){
                Peer.getStorage().removePeerWithBackup(file, ((DeletedMessage) received).getChordNodeSender());
                //System.out.println(Peer.getStorage().getBackedFiles().get(i).getCurrentRepDegree());

                if(Peer.getStorage().getBackedFiles().get(i).getCurrentRepDegree() == 0) {
                    Peer.getStorage().getBackedFiles().remove(Peer.getStorage().getBackedFiles().get(i));
                    System.out.println("\n\n-------------------------------------------------");
                    System.out.println("FILE DELETED SUCCESSFULLY");
                    System.out.println("-------------------------------------------------");
                    System.out.println("Filename: " + file.getOriginalName() + "\n");
                    break;
                }

            }
        }

    }

    private void handleDelete() {

        FileC fileReceived = received.getFile();
        for(int i = 0; i < Peer.getStorage().getStoredFiles().size(); i++){
            if(Peer.getStorage().getStoredFiles().get(i).equals(fileReceived)){
                //System.out.println("aqui2");
                String filepath = "../src/peersfiles/" + Peer.getChordID() + "/" + fileReceived.getFile_id();
                File file = new File(filepath);

                long size = file.length();
                String filename = file.getName();
                if(file.delete()) {
                    System.out.println("\nDeleted " + filename);
                    System.out.println("Delete File Size: " + size);
                    Peer.getStorage().setSpaceAvailable(Peer.getStorage().getSpaceAvailable() + size);
                    System.out.println("New Space Available: " + Peer.getStorage().getSpaceAvailable() + "\n");
                    Peer.getStorage().getStoredFiles().remove(Peer.getStorage().getStoredFiles().get(i));
                    i--;

                    DeletedMessage msg = new DeletedMessage(ChordNode.peerAddress, fileReceived, ChordNode.self);
                    ObjectOutputStream out;
                    try {
                        out = new ObjectOutputStream(clientSocket.getOutputStream());
                        out.writeObject(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void handleBackup() {
        FileC rcvd_file = received.getFile();
        if (rcvd_file.getOriginalPeer().equals(ChordNode.self)) {
            try {
                StoredMessage msg = new StoredMessage(ChordNode.peerAddress, rcvd_file);
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.writeObject(msg);
            } catch (IOException e) {
                System.out.println("Peer has disconnected!");
                BackupMessage msg = new BackupMessage(ChordNode.peerAddress, rcvd_file);
                Peer.getExec().execute(new SendMessageThread(msg, null, ChordNode.getSuccessor().getSocketAddress()));
            }
            return;
        }

        if (Peer.getStorage().getStoredFiles().contains(rcvd_file) || Peer.getStorage().getSpaceAvailable() < rcvd_file.getSize()) {
            BackupMessage msg = new BackupMessage(ChordNode.peerAddress, rcvd_file);
            Peer.getExec().execute(new SendMessageThread(msg, clientSocket, ChordNode.getSuccessor().getSocketAddress()));
            return;
        }

        String filename = "../src/peersfiles/" + Peer.getChordID() + "/" + received.getFile().getFile_id();
        try {
            // Storing chunk...
            File file = new File(filename);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            byte[] body = received.getFile().getBody();

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                if (body != null) {
                    fos.write(body);
                    rcvd_file.addBackup(ChordNode.self);
                    Peer.getStorage().addStoredFile(rcvd_file);

                    System.out.println("\n\n-------------------------------------------------------------------");
                    System.out.println("FILE STORED SUCCESSFULLY");
                    System.out.println("-------------------------------------------------------------------");
                    System.out.println("Pathname: " + rcvd_file.getPath());
                    System.out.println("Backup ID: " + rcvd_file.getFile_id());
                    System.out.println("-------------------------------------------------------------------\n");

                    if (rcvd_file.getCurrentRepDegree() >= rcvd_file.getRepDegree()) {
                        try {
                            StoredMessage msg = new StoredMessage(ChordNode.peerAddress, rcvd_file);
                            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                            out.writeObject(msg);
                        } catch (IOException e) {
                            System.out.println("\nPeer disconnected! Updating Parent Peer...");
                            UpdateFileMessage msg = new UpdateFileMessage(ChordNode.peerAddress, rcvd_file);
                            Peer.getExec().execute(new SendMessageThread(msg, null, rcvd_file.getOriginalPeer().getSocketAddress()));
                        }
                    }
                    else {
                        rcvd_file.setBody(body);
                        BackupMessage msg = new BackupMessage(ChordNode.peerAddress, rcvd_file);
                        Peer.getExec().execute(new SendMessageThread(msg, clientSocket, ChordNode.getSuccessor().getSocketAddress()));
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleStored() {
        FileC rcvd_file = received.getFile();

        if (rcvd_file.getParentPeer().equals(ChordNode.self) || rcvd_file.getOriginalPeer().equals(ChordNode.self)) {
            if (rcvd_file.getParentPeer().equals(rcvd_file.getOriginalPeer())) {
                Peer.getStorage().addBackedFile(rcvd_file);

                System.out.println("\n\n-------------------------------------------------------------------");
                System.out.println("FILE BACKED UP SUCCESSFULLY");
                System.out.println("-------------------------------------------------------------------");
                System.out.println("Pathname: " + rcvd_file.getPath());
                System.out.println("Backup ID: " + rcvd_file.getFile_id());
                System.out.println("Desired Replication Degree: " + rcvd_file.getRepDegree());
                System.out.println("Perceived Replication Degree: " + rcvd_file.getCurrentRepDegree());
                System.out.println("Peers with this File: ");
                for (ChordInfo chord_info : rcvd_file.getPeersWithBackup()) {
                    System.out.println("  - " + chord_info.getChordID());
                }
                System.out.println("-------------------------------------------------------------------");
            }
            else {
                if (rcvd_file.getPeersWithBackup().isEmpty()) {
                    System.out.println("\n\n-------------------------------------------------------------------");
                    System.out.println("NO PEER ABLE TO STORE FILE " + rcvd_file.getFile_id());
                    System.out.println("-------------------------------------------------------------------");
                    System.out.println("Pathname: " + rcvd_file.getPath());
                    System.out.println("Backup ID: " + rcvd_file.getFile_id());
                    System.out.println("-------------------------------------------------------------------\n");
                }
                else {
                    System.out.println("\n\n-------------------------------------------------------------------");
                    System.out.println("FILE STORED SUCCESSFULLY BY PEER " + rcvd_file.getPeersWithBackup().get(0).getChordID());
                    System.out.println("-------------------------------------------------------------------");
                    System.out.println("Pathname: " + rcvd_file.getPath());
                    System.out.println("Backup ID: " + rcvd_file.getFile_id());
                    System.out.println("-------------------------------------------------------------------\n");
                }

                if (!ChordNode.self.equals(rcvd_file.getOriginalPeer())) {
                    UpdateFileMessage msg = new UpdateFileMessage(ChordNode.peerAddress, rcvd_file);
                    Peer.getExec().execute(new SendMessageThread(msg, null, rcvd_file.getOriginalPeer().getSocketAddress()));
                }
                else {
                    this.handleUpdateFile();
                }
            }
        }
        else {
            try {
                StoredMessage msg = new StoredMessage(ChordNode.peerAddress, rcvd_file);
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.writeObject(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRestore() {
        FileC rcvd_file = received.getFile();

        try {
            if (ChordNode.self.equals(rcvd_file.getParentPeer())) {
                String filename = "../src/peersfiles/" + Peer.getChordID() + "/" + received.getFile().getFile_id();
                File file = new File(filename);
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                else return;

                byte[] body = received.getFile().getBody();

                try (FileOutputStream fos = new FileOutputStream(filename)) {
                    if (body != null) {
                        fos.write(body);
                        System.out.println("\n\n-------------------------------------------------------------------");
                        System.out.println("FILE RESTORED SUCCESSFULLY");
                        System.out.println("-------------------------------------------------------------------");
                        System.out.println("Pathname: " + rcvd_file.getPath());
                        System.out.println("Backup ID: " + rcvd_file.getFile_id());
                        System.out.println("Desired Replication Degree: " + rcvd_file.getRepDegree());
                        System.out.println("Perceived Replication Degree: " + rcvd_file.getCurrentRepDegree());
                        System.out.println("Peers with this File: ");
                        for (ChordInfo chord_info: rcvd_file.getPeersWithBackup()) {
                            System.out.println("  - " + chord_info.getChordID());
                        }
                        System.out.println("-------------------------------------------------------------------\n");
                    }
                }
            }
            else {
                String filename = "../src/peersfiles/" + Peer.getChordID() + "/" + received.getFile().getFile_id();
                FileInputStream fileInput = new FileInputStream(filename);
                byte[] body = fileInput.readAllBytes();
                rcvd_file.setBody(body);
                RestoreMessage msg = new RestoreMessage(ChordNode.peerAddress, rcvd_file);
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.writeObject(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUpdateFile() {
        FileC rcvd_file = received.getFile();
        try {
            if (!rcvd_file.getParentPeer().equals(ChordNode.self)) {
                for (FileC file : Peer.getStorage().getBackedFiles()) {
                    if (file.getFile_id().equals(rcvd_file.getFile_id())) {
                        file.removePeer(rcvd_file.getParentPeer());
                        if (!rcvd_file.getPeersWithBackup().isEmpty())
                            file.addPeer(rcvd_file.getPeersWithBackup().get(0));

                        System.out.println("\n\n-------------------------------------------------------------------");
                        System.out.println("FILE STATE UPDATED");
                        System.out.println("-------------------------------------------------------------------");
                        System.out.println("Pathname: " + file.getPath());
                        System.out.println("Backup ID: " + file.getFile_id());
                        System.out.println("Desired Replication Degree: " + file.getRepDegree());
                        System.out.println("Perceived Replication Degree: " + file.getCurrentRepDegree());
                        System.out.println("Peers with this File: ");
                        for (ChordInfo chord_info: file.getPeersWithBackup()) {
                            System.out.println("  - " + chord_info.getChordID());
                        }
                        System.out.println("-------------------------------------------------------------------\n");

                        if (!ChordNode.self.equals(rcvd_file.getOriginalPeer())) {
                            UpdateFileMessage msg = new UpdateFileMessage(ChordNode.peerAddress, rcvd_file);
                            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                            out.writeObject(msg);
                        }
                        break;
                    }
                }
            }
            else {
                System.out.println("Parent peer was updated!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
