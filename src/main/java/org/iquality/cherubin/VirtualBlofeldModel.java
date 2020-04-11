package org.iquality.cherubin;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import javax.sound.midi.SysexMessage;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VirtualBlofeldModel extends SoundEditorModel {

    public static final int BANKS_NUMBER = 8;
    public static final int BANK_SIZE = 128;

    static final SingleSound EMPTY_SOUND = new SingleSound(-1, "Empty", SoundCategory.Init, null, "");
    static final MultiSound EMPTY_MULTI = new MultiSound(0, "Empty", null, "");
    public static final String INIT_BLOFELD = "INIT";

    private VirtualBlofeld blofeld;
    private final PreparedStatement blofeldNamesStm;
    private final PreparedStatement loadBlofeldStm;
    private final PreparedStatement insertBlofeldStm;
    private final PreparedStatement deleteBlofeldStm;
    private final PreparedStatement loadBankStm;
    private final PreparedStatement deleteBanksStm;
    private final PreparedStatement insertSoundStm;
    private final SoundSender soundSender;

    public VirtualBlofeldModel(Connection con, AppModel appModel) {
        super(appModel);

        try {
            blofeldNamesStm = con.prepareStatement("SELECT NAME FROM BLOFELD");
            loadBlofeldStm = con.prepareStatement("SELECT ID FROM BLOFELD WHERE NAME = ?");
            insertBlofeldStm = con.prepareStatement("INSERT INTO BLOFELD (NAME) VALUES (?)");
            deleteBlofeldStm = con.prepareStatement("DELETE FROM BLOFELD WHERE ID = ?");
            loadBankStm = con.prepareStatement("SELECT BS.ID AS SOUND_ID, SLOT, BS.NAME AS SOUND_NAME, CATEGORY, SOUNDSET, SYSEX, BLOFELD_ID FROM BLOFELD_SINGLE AS BS, BLOFELD AS B WHERE B.NAME = ? AND BS.BLOFELD_ID = B.ID AND BS.BANK = ?");
            deleteBanksStm = con.prepareStatement("DELETE FROM BLOFELD_SINGLE WHERE BLOFELD_ID = ?");
            insertSoundStm = con.prepareStatement("INSERT INTO BLOFELD_SINGLE (BANK, SLOT, NAME, CATEGORY, SOUNDSET, SYSEX, BLOFELD_ID) VALUES (?, ?, ?, ?, ?, ?, ?)");
            loadBlofeld(INIT_BLOFELD);

            this.soundSender = new SoundSender(appModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void loadBlofeld(String name) {
        try {
            loadBlofeldStm.clearParameters();
            loadBlofeldStm.setString(1, name);
            int blofeldId;
            try(ResultSet resultSet = loadBlofeldStm.executeQuery()) {
                if (resultSet.next()) {
                    blofeldId = resultSet.getInt(1);
                } else {
                    throw new RuntimeException("No such virtual blofeld as " + name);
                }
            }

            List<List<SingleSound>> banks = new ArrayList<>();
            for (int bank = 0; bank < BANKS_NUMBER; bank++) {
                List<SingleSound> bankSounds = loadBank(name, bank);
                banks.add(bankSounds);
            }

            blofeld = new VirtualBlofeld(blofeldId, name, banks, initMultiBank());
            fireBlofeldChanged(blofeld);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<SingleSound> loadBank(String blofeldName, int bankNum) throws Exception {
        List<SingleSound> bank = initBank(bankNum);

        loadBankStm.clearParameters();
        loadBankStm.setString(1, blofeldName);
        loadBankStm.setInt(2, bankNum);

        try (ResultSet resultSet = loadBankStm.executeQuery()) {
            while (resultSet.next()) {
                int soundId = resultSet.getInt("SOUND_ID");
                int slot = resultSet.getInt("SLOT");
                String soundName = resultSet.getString("SOUND_NAME");
                int catOrd = resultSet.getInt("CATEGORY");
                SoundCategory category = SoundCategory.CATEGORIES[catOrd];
                Blob sysExBlob = resultSet.getBlob("SYSEX");
                byte[] sysExBytes = sysExBlob.getBytes(0, (int) sysExBlob.length());
                String soundSetName = resultSet.getString("SOUNDSET");

                SingleSound sound = new SingleSound(soundId, soundName, category, new SysexMessage(sysExBytes, sysExBytes.length), soundSetName);
                bank.set(slot, sound);
            }

            return bank;
        }
    }

    private List<SingleSound> initBank(int bank) {
        List<SingleSound> bankList = new ArrayList<>();
        for (int program = 0; program < BANK_SIZE; program++) {
            bankList.add((SingleSound) EMPTY_SOUND.clone((byte) bank, (byte) program));
        }
        return bankList;
    }

    private List<MultiSound> initMultiBank() {
        List<MultiSound> bankList = new ArrayList<>();
        for (int program = 0; program < BANK_SIZE; program++) {
            bankList.add(EMPTY_MULTI);
        }
        return bankList;
    }

    public void saveBlofeld() {
        try {
            deleteBanksStm.clearParameters();
            deleteBanksStm.setInt(1, blofeld.id);
            int deletedSounds = deleteBanksStm.executeUpdate();

            for (int bank = 0; bank < BANKS_NUMBER; bank++) {
                List<SingleSound> bankSounds = blofeld.banks.get(bank);
                for (int slot = 0; slot < BANK_SIZE; slot++) {
                    SingleSound sound = bankSounds.get(slot);
                    // "INSERT INTO BLOFELD_SINGLE (BANK, SLOT, NAME, CATEGORY, SOUNDSET, SYSEX, BLOFELD_ID)
                    if (!sound.isEmpty()) {
                        insertSoundStm.clearParameters();
                        insertSoundStm.setInt(1, bank);
                        insertSoundStm.setInt(2, slot);
                        insertSoundStm.setString(3, sound.getName());
                        insertSoundStm.setInt(4, sound.getCategory().ordinal());
                        insertSoundStm.setString(5, sound.getSoundSetName());
                        byte[] sysEx = sound.getSysEx().getMessage();
                        ByteInputStream sysExStream = new ByteInputStream(sysEx, sysEx.length);
                        insertSoundStm.setBinaryStream(6, sysExStream);
                        insertSoundStm.setInt(7, blofeld.id);

                        insertSoundStm.executeUpdate();
                    }
                }
            }

            blofeld.setDirty(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void newBlofeld(String name) {
        try {
            insertBlofeldStm.clearParameters();
            insertBlofeldStm.setString(1, name);
            insertBlofeldStm.executeUpdate();

            loadBlofeld(name);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public boolean deleteBlofeld() {
        if (blofeld.isInitial()) {
            return false;
        }
        try {
            deleteBanksStm.clearParameters();
            deleteBanksStm.setInt(1, blofeld.id);
            deleteBanksStm.executeUpdate();

            deleteBlofeldStm.clearParameters();
            deleteBlofeldStm.setInt(1, blofeld.id);
            int deleted = deleteBlofeldStm.executeUpdate();

            loadBlofeld(INIT_BLOFELD);

            return deleted > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SingleSound> getBank(int i) {
        return Collections.unmodifiableList(blofeld.banks.get(i));
    }

    public List<MultiSound> getMultiBank() {
        return blofeld.multiBank;
    }

    public VirtualBlofeld getBlofeld() {
        return blofeld;
    }

    private final List<BlofeldListener> listeners = new ArrayList<>();

    public boolean exists(String blofeldName) {
        try {
            loadBlofeldStm.clearParameters();
            loadBlofeldStm.setString(1, blofeldName);
            try (ResultSet resultSet = loadBlofeldStm.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getBlofeldNames() {
        try {
            List<String> names = new ArrayList<>();
            blofeldNamesStm.clearParameters();
            try (ResultSet resultSet = blofeldNamesStm.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
            return names;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void uploadBlofeld() {
        blofeld.banks.forEach(bank -> bank.stream().filter(Sound::nonEmpty).forEach(soundSender::sendSoundWithDelay));
    }

    public interface BlofeldListener {
        void blofeldChanged(VirtualBlofeld blofeld);
    }

    private void fireBlofeldChanged(VirtualBlofeld blofeld) {
        for (BlofeldListener listener : listeners) {
            listener.blofeldChanged(blofeld);
        }
    }

    public void addBlofeldListener(BlofeldListener listener) {
        listeners.add(listener);
    }

    public void removeBlofeldListener(BlofeldListener listener) {
        listeners.remove(listener);
    }

    public void sendSoundOn(SingleSound sound) {
        soundSender.probeNoteOff();
        soundSender.sendSound(sound, true);
        soundSender.probeNoteOn();
    }

    public void sendSoundOff() {
        soundSender.probeNoteOff();
    }

    public void sendSound(SingleSound sound, AppModel.OutputDirection outputDirection) {
        soundSender.sendSound(sound, true, outputDirection);
    }

}
