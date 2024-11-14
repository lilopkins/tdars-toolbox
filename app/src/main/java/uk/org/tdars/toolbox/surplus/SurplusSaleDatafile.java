package uk.org.tdars.toolbox.surplus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

@Getter
@Setter
public class SurplusSaleDatafile implements Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDate auctionDate;
    @Setter(value = AccessLevel.NONE)
    private ArrayList<String> callsigns;
    /**
     * A map of lot numbers to items
     */
    @Setter(value = AccessLevel.NONE)
    private HashMap<String, SurplusSaleItem> items;

    public SurplusSaleDatafile() {
        this.auctionDate = LocalDate.now();
        this.callsigns = new ArrayList<>();
        this.items = new HashMap<>();
    }

    /**
     * Save this file to the specified path
     * @param targetPath the path to save the file to
     * @throws IOException if the file couldn't be written.
     */
    public void save(File targetPath) throws IOException {
        @Cleanup val fos = new FileOutputStream(targetPath);
        @Cleanup val oos = new ObjectOutputStream(fos);
        oos.writeObject(this);
        oos.flush();
    }

    public static SurplusSaleDatafile open(File file) throws IOException, NotAnAuctionException {
        @Cleanup val fis = new FileInputStream(file);
        @Cleanup val ois = new ObjectInputStream(fis);
        try {
            val obj = ois.readObject();
            val auc = (SurplusSaleDatafile) obj;
            return auc;
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new NotAnAuctionException();
        }
    }
}
