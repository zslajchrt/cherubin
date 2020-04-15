package org.iquality.cherubin;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;
import java.sql.*;
import java.util.*;

/**
 * URL: jdbc:h2:/Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin/allsounds
 * Driver: org.h2.Driver
 * <p>
 * Creating DB: java -cp  ~/.m2/repository/com/h2database/h2/1.4.199/h2-1.4.199.jar org.h2.tools.Shell
 * Start Web Console: java -jar ~/.m2/repository/com/h2database/h2/1.4.199/h2-1.4.199.jar -baseDir /Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin
 * User: zbynek
 */
public class SoundDbModel extends SoundEditorModel {

    private final Connection con;
    private final PreparedStatement insertSoundStm;
    private final PreparedStatement updateSoundStm;
    private final PreparedStatement insertSynthSoundStm;
    private final PreparedStatement loadAllSoundStm;
    private final PreparedStatement loadSynthStm;
    private final PreparedStatement loadBankStm;
    private final PreparedStatement updateBankSoundStm;
    private final PreparedStatement synthInstancesStm;
    private final PreparedStatement deleteSynthStm;
    private final PreparedStatement deleteBanksStm;
    private final PreparedStatement insertSynthStm;

    private Map<String, SoundSet<Sound>> soundSetsMap = new HashMap<>();
    private List<Sound> sounds = new ArrayList<>();

    public SoundDbModel(AppModel appModel, Connection con) {
        super(appModel);

        try {
            this.con = con;
            insertSoundStm = con.prepareStatement("INSERT INTO SOUND (NAME, CATEGORY, SYSEX, SOUNDSET, TYPE) VALUES (?, ?, ?, ?, ?)");
            updateSoundStm = con.prepareStatement("UPDATE SOUND SET NAME=?, CATEGORY=?, SYSEX=?, SOUNDSET=? WHERE ID=?");
            loadAllSoundStm = con.prepareStatement("SELECT ID, NAME, CATEGORY, SYSEX, SOUNDSET, TYPE FROM SOUND");
            synthInstancesStm = con.prepareStatement("SELECT ID, NAME, TYPE FROM SYNTH");
            loadSynthStm = con.prepareStatement("SELECT ID, TYPE FROM SYNTH WHERE NAME = ?");
            loadBankStm = con.prepareStatement("SELECT SN.ID AS SOUND_ID, SLOT, SN.NAME AS SOUND_NAME, CATEGORY, SOUNDSET, SYSEX, SYNTH_ID FROM SYNTH_SOUND AS SN, SYNTH AS S WHERE S.NAME = ? AND SN.SYNTH_ID = S.ID AND SN.BANK = ?");
            updateBankSoundStm = con.prepareStatement("UPDATE SYNTH_SOUND SET NAME=?, CATEGORY=?, SYSEX=?, SOUNDSET=? WHERE ID=?");
            deleteSynthStm = con.prepareStatement("DELETE FROM SYNTH WHERE ID = ?");
            deleteBanksStm = con.prepareStatement("DELETE FROM SYNTH_SOUND WHERE SYNTH_ID = ?");
            insertSynthStm = con.prepareStatement("INSERT INTO SYNTH (NAME, TYPE) VALUES (?, ?)");
            insertSynthSoundStm = con.prepareStatement("INSERT INTO SYNTH_SOUND (BANK, SLOT, NAME, CATEGORY, SOUNDSET, SYSEX, SYNTH_ID) VALUES (?, ?, ?, ?, ?, ?, ?)");
            loadSoundSets();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public AppModel getAppModel() {
        return appModel;
    }

    public Collection<SoundSet<Sound>> getSoundSets() {
        return Collections.unmodifiableCollection(soundSetsMap.values());
    }

    public List<SynthHeader> getSynthModels() {
        try {
            List<SynthHeader> names = new ArrayList<>();
            synthInstancesStm.clearParameters();
            try (ResultSet resultSet = synthInstancesStm.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt(1);
                    String name = resultSet.getString(2);
                    String type = resultSet.getString(3);
                    names.add(new SynthHeader(id, name, SynthFactoryRegistry.INSTANCE.getSynthFactory(type)));
                }
            }
            return names;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Sound> getSounds() {
        return Collections.unmodifiableList(sounds);
    }

    public Synth newSynth(SynthFactory synthFactory, String name) {
        try {
            insertSynthStm.clearParameters();
            insertSynthStm.setString(1, name);
            insertSynthStm.setString(2, synthFactory.getSynthId());
            insertSynthStm.executeUpdate();

            return loadSynth(new SynthHeader(-1, name, synthFactory));
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private void loadSoundSets() {
        this.soundSetsMap = new HashMap<>();
        try {
            ResultSet resultSet = loadAllSoundStm.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                int catOrd = resultSet.getInt(3);
                SoundCategory category = SoundCategory.CATEGORIES[catOrd];
                Blob sysExBlob = resultSet.getBlob(4);
                byte[] sysExBytes = sysExBlob.getBytes(0, (int) sysExBlob.length());
                String soundSetName = resultSet.getString(5);
                String synthId = resultSet.getString(6);

                SynthFactory synthFactory = getSoundFactory(synthId);
                Sound sound = synthFactory.createSingleSound(id, name, new SysexMessage(sysExBytes, sysExBytes.length), category, soundSetName);
                SoundSet<Sound> soundSet = soundSetsMap.computeIfAbsent(soundSetName, SoundSet::new);
                soundSet.sounds.add(sound);
                sounds.add(sound);
            }
        } catch (SQLException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveSynth(Synth synth) {
        try {
            deleteBanksStm.clearParameters();
            deleteBanksStm.setInt(1, synth.getId());
            int deletedSounds = deleteBanksStm.executeUpdate();

            for (int bank = 0; bank < synth.getBanks().size(); bank++) {
                List<Sound> bankSounds = synth.getBanks().get(bank);
                saveBank(synth, bank, bankSounds);
            }

            List multiSounds = synth.getMulti();
            saveBank(synth, -1, multiSounds);

            synth.setDirty(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveBank(Synth synth, int bank, List<Sound> bankSounds) throws SQLException {
        for (int slot = 0; slot < bankSounds.size(); slot++) {
            Sound sound = bankSounds.get(slot);
            // "INSERT INTO BLOFELD_SINGLE (BANK, SLOT, NAME, CATEGORY, SOUNDSET, SYSEX, BLOFELD_ID)
            if (!sound.isEmpty()) {
                insertSynthSoundStm.clearParameters();
                insertSynthSoundStm.setInt(1, bank);
                insertSynthSoundStm.setInt(2, slot);
                insertSynthSoundStm.setString(3, sound.getName());
                insertSynthSoundStm.setInt(4, sound.getCategory().ordinal());
                insertSynthSoundStm.setString(5, sound.getSoundSetName());
                byte[] sysEx = sound.getSysEx().getMessage();
                ByteInputStream sysExStream = new ByteInputStream(sysEx, sysEx.length);
                insertSynthSoundStm.setBinaryStream(6, sysExStream);
                insertSynthSoundStm.setInt(7, synth.getId());

                insertSynthSoundStm.executeUpdate();
            }
        }
    }

    public SynthFactory getSoundFactory(String synthId) {
        return SynthFactoryRegistry.INSTANCE.getSynthFactory(synthId);
    }

    public Synth loadSynth(SynthHeader synthHeader) {
        try {
            loadSynthStm.clearParameters();
            loadSynthStm.setString(1, synthHeader.getName());
            int synthId;
            String synthType;
            try (ResultSet resultSet = loadSynthStm.executeQuery()) {
                if (resultSet.next()) {
                    synthId = resultSet.getInt(1);
                    synthType = resultSet.getString(2);
                    assert synthHeader.getSynthFactory().getSynthId().equals(synthType);
                } else {
                    throw new RuntimeException("No such synth instance as " + synthHeader);
                }
            }

            SynthFactory synthFactory = SynthFactoryRegistry.INSTANCE.getSynthFactory(synthType);

            List<List<Sound>> banks = new ArrayList<>();
            for (int bank = 0; bank < synthFactory.getBankCount(); bank++) {
                List<Sound> bankSounds = loadBank(synthFactory, synthHeader.getName(), bank);
                banks.add(bankSounds);
            }
            List multiSounds = loadBank(synthFactory, synthHeader.getName(), -1);

            return new Synth(synthId, synthHeader.getName(), banks, multiSounds, synthFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public List<Sound> loadBank(SynthFactory synthFactory, String synthName, int bankNum) throws Exception {
        List bank = bankNum >= 0 ? synthFactory.createBank(bankNum) : synthFactory.createMultiBank();

        loadBankStm.clearParameters();
        loadBankStm.setString(1, synthName);
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

                Sound sound = synthFactory.createSingleSound(soundId, soundName, new SysexMessage(sysExBytes, sysExBytes.length), category, soundSetName);
                bank.set(slot, sound);
            }

            return bank;
        }
    }

    public boolean synthExists(String synthName) {
        try {
            loadSynthStm.clearParameters();
            loadSynthStm.setString(1, synthName);
            try (ResultSet resultSet = loadSynthStm.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteSynth(Synth synth) {
        try {
            deleteBanksStm.clearParameters();
            deleteBanksStm.setInt(1, synth.getId());
            deleteBanksStm.executeUpdate();

            deleteSynthStm.clearParameters();
            deleteSynthStm.setInt(1, synth.getId());
            int deleted = deleteSynthStm.executeUpdate();

            return deleted > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void insertSoundSet(SoundSet<Sound> soundSet) {
        try {
            for (Sound sound : soundSet.sounds) {
                insertSound(sound);
            }
            loadSoundSets();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertSound(Sound sound) throws SQLException {
        insertSoundStm.clearParameters();
        insertSoundStm.setString(1, sound.getName());
        insertSoundStm.setInt(2, sound.getCategory().ordinal());
        byte[] sysEx = sound.getSysEx().getMessage();
        ByteInputStream sysExStream = new ByteInputStream(sysEx, sysEx.length);
        insertSoundStm.setBinaryStream(3, sysExStream);
        insertSoundStm.setString(4, sound.getSoundSetName());
        insertSoundStm.setString(5, sound.getSynthFactory().getSynthId());
        insertSoundStm.executeUpdate();
    }

    public void updateSound(Sound sound) {
        updateSound(sound, updateSoundStm);
    }

    public void updateBankSound(SingleSound sound) {
        updateSound(sound, updateBankSoundStm);
    }

    public void updateBankSound(MultiSound sound) {
        updateSound(sound, updateBankSoundStm);
    }

    private void updateSound(Sound sound, PreparedStatement stm) {
        try {
            stm.clearParameters();

            //"UPDATE SOUND SET NAME=?, CATEGORY=?, SYSEX=?, SOUNDSET=?) WHERE ID=?
            stm.setString(1, sound.getName());
            stm.setInt(2, sound.getCategory().ordinal());
            byte[] sysEx = sound.getSysEx().getMessage();
            ByteInputStream sysExStream = new ByteInputStream(sysEx, sysEx.length);
            stm.setBinaryStream(3, sysExStream);
            stm.setString(4, sound.getSoundSetName());
            stm.setInt(5, sound.getId());
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static SoundSet<Sound> midiEventsToSoundSet(MidiEvents events, String soundSetName) {
        SoundSet<Sound> soundSet =  new SoundSet<>(soundSetName);

        for (MidiEvent event : events.getEvents()) {
            if (!(event.getMessage() instanceof SysexMessage)) {
                continue;
            }
            SysexMessage message = (SysexMessage) event.getMessage();
            SynthFactory synthFactory = SynthFactoryRegistry.INSTANCE.getSynthFactory(message);
            if (synthFactory != null) {
                List<Sound> sound = synthFactory.createSounds(message, soundSetName);
                soundSet.sounds.addAll(sound);
            } else {
                return null;
            }
        }
        return soundSet;
    }

    public void close() {
        try {
            con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        String url = "jdbc:h2:/Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin/allsounds;IFEXISTS=TRUE";
        String user = "zbynek";
        String passwd = "Ovation1";

        String query = "SELECT * FROM SOUND";
        //String query = "SELECT * FROM INFORMATION_SCHEMA.TABLES";

        try (Connection con = DriverManager.getConnection(url, user, passwd);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                System.out.println(rs.getString(3));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
