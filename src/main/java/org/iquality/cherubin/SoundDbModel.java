package org.iquality.cherubin;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
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
public class SoundDbModel {

    private final AppModel appModel;
    private final Connection con;
    private final PreparedStatement insertSoundStm;
    private final PreparedStatement loadAllSoundStm;

    private Map<String, SoundSet<SingleSound>> soundSetsMap = new HashMap<>();
    private List<SingleSound> sounds = new ArrayList<>();

    public SoundDbModel(AppModel appModel, Connection con) {
        this.appModel = appModel;
        try {
            this.con = con;
            insertSoundStm = con.prepareStatement("INSERT INTO SOUND (NAME, CATEGORY, SYSEX, SOUNDSET) VALUES (?, ?, ?, ?)");
            loadAllSoundStm = con.prepareStatement("SELECT ID, NAME, CATEGORY, SYSEX, SOUNDSET FROM SOUND");

            loadSoundSets();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public AppModel getAppModel() {
        return appModel;
    }

    public Collection<SoundSet<SingleSound>> getSoundSets() {
        return Collections.unmodifiableCollection(soundSetsMap.values());
    }

    public List<SingleSound> getSounds() {
        return Collections.unmodifiableList(sounds);
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

                SingleSound sound = new SingleSound(id, name, category, new SysexMessage(sysExBytes, sysExBytes.length), soundSetName);
                SoundSet<SingleSound> soundSet = soundSetsMap.computeIfAbsent(soundSetName, SoundSet::new);
                soundSet.sounds.add(sound);
                sounds.add(sound);
            }
        } catch (SQLException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertSoundSet(SoundSet<SingleSound> soundSet) {
        try {
            for (SingleSound sound : soundSet.sounds) {
                insertSound(sound);
            }
            loadSoundSets();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertSound(SingleSound sound) throws SQLException {
        insertSoundStm.setString(1, sound.name);
        insertSoundStm.setInt(2, sound.category.ordinal());
        byte[] sysEx = sound.sysEx.getMessage();
        ByteInputStream sysExStream = new ByteInputStream(sysEx, sysEx.length);
        insertSoundStm.setBinaryStream(3, sysExStream);
        insertSoundStm.setString(4, sound.soundSetName);
        insertSoundStm.executeUpdate();
        insertSoundStm.clearParameters();
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
