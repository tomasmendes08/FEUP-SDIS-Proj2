package files;

import chord.ChordInfo;
import chord.ChordNode;
import peers.Peer;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class FileC implements Serializable {
    private ChordInfo originalPeer;
    private String path;
    private String originalName;
    private String file_id;
    private File file;
    private byte[] body;
    private int repDegree;
    private List<ChordInfo> peersWithBackup;
    private ChordInfo parentPeer;
    private long size;

    public FileC(String path, int repDegree) {
        this.file = new File(path);
        this.path = path;
        this.size = file.length();
        this.originalName = file.getName();
        this.repDegree = repDegree;
        this.peersWithBackup = new ArrayList<>();
        this.generateFileID();
    }

    public FileC(FileC file) {
        this.file = file.getFile();
        this.path = file.getPath();
        this.size = file.getSize();
        this.originalName = file.getOriginalName();
        this.repDegree = file.getRepDegree();
        this.peersWithBackup = file.getPeersWithBackup();
        this.originalPeer = ChordNode.self;
        this.file_id = file.getFile_id();
    }

    public String getPath() {
        return path;
    }

    public File getFile() {
        return file;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getFile_id() {
        return file_id;
    }

    public byte[] getBody() {
        return body;
    }

    public long getSize() {
        return size;
    }

    public int getRepDegree() {
        return repDegree;
    }

    public ChordInfo getOriginalPeer() {
        return originalPeer;
    }

    public List<ChordInfo> getPeersWithBackup() {
        return peersWithBackup;
    }

    public int getCurrentRepDegree() {
        return peersWithBackup.size();
    }


    public ChordInfo getParentPeer() {
        return parentPeer;
    }

    public void addBackup(ChordInfo chordID) {
        peersWithBackup.add(chordID);
    }
    
    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setParentPeer(ChordInfo peer) {
        this.parentPeer = peer;
    }

    public void setOriginalPeer(ChordInfo originalPeer) {
        this.originalPeer = originalPeer;
    }

    public void setRepDegree(int repDegree) {
        this.repDegree = repDegree;
    }

    public void clearPeers() {
        this.peersWithBackup = new ArrayList<>();
    }

    public void removePeer(ChordInfo peer) {
        this.peersWithBackup.remove(peer);
    }

    public void addPeer(ChordInfo peer) {
        this.peersWithBackup.add(peer);
    }

    // Used for hashing
    public String toHexString(byte[] hash)
    {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    private void generateFileID() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
            String metadata = Peer.getChordID() + this.file.getAbsolutePath() + this.file.lastModified();
            this.file_id = toHexString(md.digest(metadata.getBytes(StandardCharsets.UTF_8))) + "_" + this.file.getName();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }



    @Override
    public String toString() {
        return "FileC{" +
                "path='" + path + '\'' +
                ", file_id='" + file_id + '\'' +
                ", file=" + file +
                ", repDegree=" + repDegree +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileC fileC = (FileC) o;
        return Objects.equals(file_id, fileC.file_id);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(originalPeer, path, originalName, file_id, file, repDegree, peersWithBackup, parentPeer, size);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }

    public int sortFiles() {
        int priority = 0;
        /*
         * Sort is done by:
         *   (1) smallest replication degree => higher chance of finding a peer that can store the file
         *   (2) biggest size
         * */
        priority += (this.repDegree * 100000);
        priority += this.size;

        return priority;
    }
}