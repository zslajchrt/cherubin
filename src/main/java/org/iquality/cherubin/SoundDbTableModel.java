package org.iquality.cherubin;

import org.japura.gui.event.ListCheckListener;
import org.japura.gui.event.ListEvent;

import javax.swing.table.AbstractTableModel;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SoundDbTableModel extends AbstractTableModel {
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_CATEGORY = 2;
    public static final int COLUMN_SOUNDSET = 3;
    public static final int COLUMN_SYNTH = 4;
    public static final int COLUMN_CREATED = 5;
    public static final int COLUMN_NOTE = 6;
    public static final Timestamp NULL_TS = new Timestamp(0);

    private final SoundDbModel soundDbModel;
    private final String[] columnNames = {"Id", "Name", "Category", "Sound Set", "Synth", "Created", "Note"};
    private List<Sound> listSounds;
    private Predicate<Sound> synthFilter = s -> true;
    private Predicate<Sound> categoryFilter = s -> true;
    private Predicate<SoundSet<Sound>> soundSetFilter = s -> true;

    private final List<Consumer<Iterable<SynthFactory>>> synthListeners = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<Iterable<SoundSet<Sound>>>> soundSetsListeners = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<Iterable<SoundCategory>>> categoriesListeners = Collections.synchronizedList(new ArrayList<>());

    public SoundDbTableModel(SoundDbModel dbModel) {
        this.soundDbModel = dbModel;
        applyFiltersList();
    }

    public SoundDbModel getSoundDbModel() {
        return soundDbModel;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return listSounds.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COLUMN_ID:
                return Integer.class;
            case COLUMN_NAME:
                return Sound.class;
            case COLUMN_CATEGORY:
                return SoundCategory.class;
            case COLUMN_SOUNDSET:
                return String.class;
            case COLUMN_SYNTH:
                return String.class;
            case COLUMN_CREATED:
                return Timestamp.class;
            case COLUMN_NOTE:
                return Boolean.class;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_ID:
                return false;
            case COLUMN_NAME:
                return true;
            case COLUMN_CATEGORY:
                return true;
            case COLUMN_SOUNDSET:
                return false;
            case COLUMN_SYNTH:
                return false;
            case COLUMN_CREATED:
                return false;
            case COLUMN_NOTE:
                return false;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Sound sound = listSounds.get(rowIndex);
        Object returnValue;
        switch (columnIndex) {
            case COLUMN_ID:
                returnValue = sound.getId();
                break;
            case COLUMN_NAME:
                returnValue = sound;
                break;
            case COLUMN_CATEGORY:
                returnValue = sound.getCategory();
                break;
            case COLUMN_SOUNDSET:
                returnValue = sound.getSoundSetName();
                break;
            case COLUMN_SYNTH:
                returnValue = sound.getSynthFactory().getSynthId();
                break;
            case COLUMN_CREATED:
                returnValue = SoundMeta.getCreated(sound, NULL_TS);
                break;
            case COLUMN_NOTE:
                String note = SoundMeta.getNote(sound, null);
                returnValue = note != null;
                break;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Sound sound;
        switch (columnIndex) {
            case COLUMN_NAME:
                sound = listSounds.get(rowIndex);
                sound.setName((String) value);
                soundDbModel.updateSound(sound);
                break;
            case COLUMN_CATEGORY:
                sound = listSounds.get(rowIndex);
                sound.setCategory((SoundCategory) value);
                soundDbModel.updateSound(sound);
                break;
            default:
                break;
        }
    }

    public void updateSound(Sound sound) {
        soundDbModel.updateSound(sound);
        fireTableDataChanged();
    }

    public void deleteSound(Sound sound) {
        listSounds.remove(sound);
        soundDbModel.deleteSound(sound);
        fireTableDataChanged();
    }

    public void addSound(Sound sound) {
        soundDbModel.insertOneSound(sound);
        listSounds.add(sound);
        fireTableDataChanged();
    }

    private void applyFiltersList() {
        listSounds = soundDbModel.getSoundSets().stream().filter(soundSetFilter).map(soundSet -> soundSet.sounds.stream().filter(categoryFilter).filter(synthFilter)).flatMap(s -> s).collect(Collectors.toList());
        fireTableDataChanged();
    }

    private void applySynthFilter(List<Object> checkedSynths) {
        synthFilter = checkedSynths.isEmpty() ? sound -> true : sound -> checkedSynths.contains(sound.getSynthFactory());
        applyFiltersList();
    }

    private void applyCategoryFilter(List<Object> checkedCategories) {
        categoryFilter = checkedCategories.isEmpty() ? sound -> true : sound -> checkedCategories.contains(sound.getCategory());
        applyFiltersList();
    }

    private void applySoundSetFilter(List<Object> checkedSoundSets) {
        soundSetFilter = checkedSoundSets.isEmpty() ? soundSet -> true : checkedSoundSets::contains;
        applyFiltersList();
    }

    public ListCheckListener getSynthFilterListener() {
        return new ListCheckListener() {
            @Override
            public void removeCheck(ListEvent listEvent) {
                applySynthFilter(listEvent.getSource().getCheckeds());
            }

            @Override
            public void addCheck(ListEvent listEvent) {
                applySynthFilter(listEvent.getSource().getCheckeds());
            }
        };
    }

    public ListCheckListener getCategoryFilterListener() {
        return new ListCheckListener() {
            @Override
            public void removeCheck(ListEvent listEvent) {
                applyCategoryFilter(listEvent.getSource().getCheckeds());
            }

            @Override
            public void addCheck(ListEvent listEvent) {
                applyCategoryFilter(listEvent.getSource().getCheckeds());
            }
        };
    }

    public ListCheckListener getSoundSetFilterListener() {
        return new ListCheckListener() {
            @Override
            public void removeCheck(ListEvent listEvent) {
                applySoundSetFilter(listEvent.getSource().getCheckeds());
            }

            @Override
            public void addCheck(ListEvent listEvent) {
                applySoundSetFilter(listEvent.getSource().getCheckeds());
            }
        };
    }

    public void addSoundSet(SoundSet<Sound> soundSet) {
        soundDbModel.insertSoundSet(soundSet);
        applyFiltersList();
        fireSoundSetsChanged();
    }

    public void fire() {
        fireSynthsChanged();
        fireCategoriesChanged();
        fireSoundSetsChanged();
    }

    private void fireSynthsChanged() {
        for (Consumer<Iterable<SynthFactory>> synthsListener : synthListeners) {
            synthsListener.accept(SynthFactoryRegistry.INSTANCE.getSynthFactories());
        }
    }

    private void fireCategoriesChanged() {
        for (Consumer<Iterable<SoundCategory>> categoriesListener : categoriesListeners) {
            categoriesListener.accept(Arrays.asList(SoundCategory.values()));
        }
    }

    private void fireSoundSetsChanged() {
        for (Consumer<Iterable<SoundSet<Sound>>> soundSetsListener : soundSetsListeners) {
            soundSetsListener.accept(soundDbModel.getSoundSets());
        }
    }

    public Consumer<Consumer<Iterable<SynthFactory>>> getSynthNotifier() {
        return synthListeners::add;
    }

    public Consumer<Consumer<Iterable<SoundCategory>>> getCategoriesNotifier() {
        return categoriesListeners::add;
    }

    public Consumer<Consumer<Iterable<SoundSet<Sound>>>> getSoundSetsNotifier() {
        return soundSetsListeners::add;
    }

    public void close() {
        this.soundDbModel.close();
    }
}
