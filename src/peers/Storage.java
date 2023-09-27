package peers;

import chord.ChordInfo;
import communication.protocols.DeletedMessage;
import files.FileC;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class Storage implements Serializable {
    private final long peer_id;
    private final ArrayList<FileC> backedFiles; // Backed Files List
    private final ArrayList<FileC> storedFiles; // Backed Files List
    private long capacity;
    private long spaceavailable;

    public Storage(long peer_id) throws IOException, ClassNotFoundException {
        this.peer_id = peer_id;
        this.backedFiles = new ArrayList<>();
        this.storedFiles = new ArrayList<>();
        this.capacity = 64000000000L;
        this.spaceavailable = this.capacity;
    }

    public ArrayList<FileC> getBackedFiles() {
        return backedFiles;
    }

    public ArrayList<FileC> getStoredFiles() {
        return storedFiles;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getSpaceAvailable() {
        return spaceavailable;
    }

    public void addBackedFile(FileC rcvd_file) {
        rcvd_file.setBody(null); // File is not supposed to be stored in volatile memory
        this.backedFiles.add(rcvd_file);
    }

    public void addStoredFile(FileC rcvd_file) {
        rcvd_file.setBody(null); // File is not supposed to be stored in volatile memory
        this.storedFiles.add(rcvd_file);
        this.spaceavailable -= rcvd_file.getSize();
    }

    public Long getSpaceOccupied() {
        long space = 0;
        for (FileC file: storedFiles)
            space += file.getSize();

        return space;

    }

    public void setCapacity(long space) {
        this.capacity = space;
    }

    public void setSpaceAvailable(long spaceAvailable) {
        this.spaceavailable = spaceAvailable;
    }


    public void removePeerWithBackup(FileC file, ChordInfo chordNodeSender) {
        for(FileC fileC : this.backedFiles){
            if(fileC.equals(file)){
                fileC.getPeersWithBackup().remove(chordNodeSender);
            }
        }
    }

    public boolean checkIfAlreadyBackedUp(String filePath) {
        for (FileC file: backedFiles) {
            if (file.getPath().equals(filePath)) {
                if (file.getRepDegree() == 0) {
                    System.out.println("This file might still stored in at least one peer, even though its deletion was requested\nIgnoring that/those peer(s) and re-backing up the file...");
                    backedFiles.remove(file);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public void removeFile(FileC f) {
        String filename = "../src/peersfiles/" + Peer.getChordID() + "/" + f.getFile_id();
        File file = new File(filename);
        if(file.delete()) {
            System.out.println("Deleted File " + f.getFile_id());
            System.out.println("Delete File Size: " + f.getSize() + "\n");
            storedFiles.remove(f);
        }
        else {
            System.out.println("Unable to delete the file " + f.getFile_id());
        }
    }
}
