package org.iquality.cherubin;

import org.japura.gui.event.ListCheckListener;
import org.japura.gui.event.ListEvent;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SingleSoundTableModel extends AbstractTableModel {
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_CATEGORY = 2;
    public static final int COLUMN_SOUNDSET = 3;

    private final DbManager dbManager;
    private final String[] columnNames = {"Id", "Name", "Category", "Sound Set"};
    private Collection<SoundSet<SingleSound>> soundSets;
    private List<SingleSound> listSounds;
    private Predicate<SingleSound> categoryFilter = s -> true;
    private Predicate<SoundSet<SingleSound>> soundSetFilter = s -> true;

    private final List<Consumer<Iterable<SoundSet<SingleSound>>>> soundSetsListeners = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<Iterable<SoundCategory>>> categoriesListeners = Collections.synchronizedList(new ArrayList<>());

    public SingleSoundTableModel(DbManager dbManager) {
        this.dbManager = dbManager;
        this.soundSets = dbManager.loadSoundSets();
        applyFiltersList();
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
        if (listSounds.isEmpty()) {
            return Object.class;
        }
        return getValueAt(0, columnIndex).getClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SingleSound sound = listSounds.get(rowIndex);
        Object returnValue;
        switch (columnIndex) {
            case COLUMN_ID:
                returnValue = sound.id;
                break;
            case COLUMN_NAME:
                returnValue = sound;
                break;
            case COLUMN_CATEGORY:
                returnValue = sound.category;
                break;
            case COLUMN_SOUNDSET:
                returnValue = sound.soundSet.name;
                break;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

    private void applyFiltersList() {
        listSounds = soundSets.stream().filter(soundSetFilter).map(soundSet -> soundSet.sounds.stream().filter(categoryFilter)).flatMap(s -> s).collect(Collectors.toList());
        fireTableDataChanged();
    }

    private void applyCategoryFilter(List<Object> checkedCategories) {
        categoryFilter = checkedCategories.isEmpty() ? sound -> true : sound -> checkedCategories.contains(sound.category);
        applyFiltersList();
    }

    private void applySoundSetFilter(List<Object> checkedSoundSets) {
        soundSetFilter = checkedSoundSets.isEmpty() ? soundSet -> true : checkedSoundSets::contains;
        applyFiltersList();
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

    public void addSoundSet(SoundSet<SingleSound> soundSet) {
        dbManager.insertSoundSet(soundSet);
        soundSets = dbManager.loadSoundSets();
        applyFiltersList();
        fireSoundSetsChanged();
    }

    public void fire() {
        fireCategoriesChanged();
        fireSoundSetsChanged();
    }

    private void fireCategoriesChanged() {
        for (Consumer<Iterable<SoundCategory>> categoriesListener : categoriesListeners) {
            categoriesListener.accept(Arrays.asList(SoundCategory.values()));
        }
    }

    private void fireSoundSetsChanged() {
        for (Consumer<Iterable<SoundSet<SingleSound>>> soundSetsListener : soundSetsListeners) {
            soundSetsListener.accept(soundSets);
        }
    }

    public Consumer<Consumer<Iterable<SoundCategory>>> getCategoriesNotifier() {
        return categoriesListeners::add;
    }

    public Consumer<Consumer<Iterable<SoundSet<SingleSound>>>> getSoundSetsNotifier() {
        return soundSetsListeners::add;
    }

    public void close() {
        this.dbManager.close();
    }
}
