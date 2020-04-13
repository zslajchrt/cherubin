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
    private final PreparedStatement insertSynthSoundStm;
    private final PreparedStatement loadAllSoundStm;
    private final PreparedStatement loadSynthStm;
    private final PreparedStatement loadBankStm;
    private final PreparedStatement synthNamesStm;
    private final PreparedStatement deleteSynthStm;
    private final PreparedStatement deleteBanksStm;
    private final PreparedStatement insertSynthStm;

    private Map<String, SoundSet<Sound>> soundSetsMap = new HashMap<>();
    private List<Sound> sounds = new ArrayList<>();

    public SoundDbModel(AppModel appModel, Connection con) {
        super(appModel);

        try {
            this.con = con;
            insertSoundStm = con.prepareStatement("INSERT INTO SOUND (NAME, CATEGORY, SYSEX, SOUNDSET) VALUES (?, ?, ?, ?)");
            loadAllSoundStm = con.prepareStatement("SELECT ID, NAME, CATEGORY, SYSEX, SOUNDSET FROM SOUND");
            synthNamesStm = con.prepareStatement("SELECT NAME FROM SYNTH");
            loadSynthStm = con.prepareStatement("SELECT ID FROM SYNTH WHERE NAME = ?");
            loadBankStm = con.prepareStatement("SELECT SN.ID AS SOUND_ID, SLOT, SN.NAME AS SOUND_NAME, CATEGORY, SOUNDSET, SYSEX, SYNTH_ID FROM SYNTH_SOUND AS SN, SYNTH AS S WHERE S.NAME = ? AND SN.SYNTH_ID = S.ID AND SN.BANK = ?");
            deleteSynthStm = con.prepareStatement("DELETE FROM SYNTH WHERE ID = ?");
            deleteBanksStm = con.prepareStatement("DELETE FROM SYNTH_SOUND WHERE SYNTH_ID = ?");
            insertSynthStm = con.prepareStatement("INSERT INTO SYNTH (NAME) VALUES (?)");
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

    public List<String> getSynthModels(String synthId) {
        try {
            List<String> names = new ArrayList<>();
            synthNamesStm.clearParameters();
            try (ResultSet resultSet = synthNamesStm.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
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

    public void newSynth(SynthFactory synthFactory, String name) {
        try {
            insertSynthStm.clearParameters();
            insertSynthStm.setString(1, name);
            insertSynthStm.executeUpdate();

            loadSynth(synthFactory, name);
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

                String soundFactoryId = "Blofeld"; // TODO
                SynthFactory synthFactory = getSoundFactory(soundFactoryId);
                Sound sound = synthFactory.createSound(id, new SysexMessage(sysExBytes, sysExBytes.length), soundSetName);
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

    public Synth loadSynth(SynthFactory synthFactory, String synthName) {
        try {
            loadSynthStm.clearParameters();
            loadSynthStm.setString(1, synthName);
            int synthId;
            try (ResultSet resultSet = loadSynthStm.executeQuery()) {
                if (resultSet.next()) {
                    synthId = resultSet.getInt(1);
                } else {
                    throw new RuntimeException("No such synth instance as " + synthName);
                }
            }

            List<List<Sound>> banks = new ArrayList<>();
            for (int bank = 0; bank < synthFactory.getBankCount(); bank++) {
                List<Sound> bankSounds = loadBank(synthFactory, synthName, bank);
                banks.add(bankSounds);
            }
            List multiSounds = loadBank(synthFactory, synthName, -1);

            return new Synth(synthId, synthName, banks, multiSounds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public List<Sound> loadBank(SynthFactory synthFactory, String synthName, int bankNum) throws Exception {
        List bank = bankNum >= 0 ? synthFactory.createBank(bankNum) : synthFactory.createMulti();

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

                Sound sound = synthFactory.createSound(soundId, new SysexMessage(sysExBytes, sysExBytes.length), soundSetName);
                //Sound sound = new Sound(soundId, new SysexMessage(sysExBytes, sysExBytes.length), soundSetName);
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
        insertSoundStm.setString(1, sound.getName());
        insertSoundStm.setInt(2, sound.getCategory().ordinal());
        byte[] sysEx = sound.getSysEx().getMessage();
        ByteInputStream sysExStream = new ByteInputStream(sysEx, sysEx.length);
        insertSoundStm.setBinaryStream(3, sysExStream);
        insertSoundStm.setString(4, sound.getSoundSetName());
        insertSoundStm.executeUpdate();
        insertSoundStm.clearParameters();
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
                Sound sound = synthFactory.createSound(-1, message, soundSetName);
                soundSet.sounds.add(sound);
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
