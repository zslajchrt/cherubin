package org.iquality.cherubin;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import javax.sound.midi.InvalidMidiDataException;
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
public class DbManager {

    private final Connection con;
    private final PreparedStatement insertSoundStm;
    private final PreparedStatement loadAllSoundStm;

    public DbManager(String url, String user, String password) {
        try {
            con = DriverManager.getConnection(url, user, password);
            insertSoundStm = con.prepareStatement("INSERT INTO SOUND (NAME, CATEGORY, SYSEX, SOUNDSET) VALUES (?, ?, ?, ?)");
            loadAllSoundStm = con.prepareStatement("SELECT ID, NAME, CATEGORY, SYSEX, SOUNDSET FROM SOUND");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<SoundSet<SingleSound>> loadSoundSets() {
        Map<String, SoundSet<SingleSound>> soundSets = new HashMap<>();
        try {
            ResultSet resultSet = loadAllSoundStm.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                int catOrd = resultSet.getInt(3);
                SoundCategory category = SoundCategory.CATEGORIES[catOrd];
                Blob sysExBlob  = resultSet.getBlob(4);
                byte[] sysExBytes = sysExBlob.getBytes(0, (int) sysExBlob.length());
                String soundSetName = resultSet.getString(5);

                SoundSet<SingleSound> soundSet = soundSets.computeIfAbsent(soundSetName, SoundSet::new);
                new SingleSound(id, name, category, new SysexMessage(sysExBytes, sysExBytes.length), soundSet);
            }
            return soundSets.values();
        } catch (SQLException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertSoundSet(SoundSet<SingleSound> soundSet) {
        try {
            for (SingleSound sound : soundSet.sounds) {
                insertSound(sound);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertSound(SingleSound sound) throws SQLException {
        insertSoundStm.setString(1, sound.name);
        insertSoundStm.setInt(2, sound.category.ordinal());
        byte[] sysEx = sound.dump.getMessage();
        ByteInputStream sysExStream = new ByteInputStream(sysEx, sysEx.length);
        insertSoundStm.setBinaryStream(3, sysExStream);
        insertSoundStm.setString(4, sound.soundSet.name);
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
