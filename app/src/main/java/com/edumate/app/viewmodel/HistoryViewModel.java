package com.edumate.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.edumate.app.model.Session;
import com.edumate.app.repository.SessionRepository;
import com.edumate.app.repository.SyncRepository;

import java.util.ArrayList;
import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;
    private final SyncRepository syncRepository;

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    private final MutableLiveData<String> syncStatusMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Session>> allSessions = new MutableLiveData<>(new ArrayList<>());
    private final MediatorLiveData<List<Session>> filteredSessions = new MediatorLiveData<>();

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        this.sessionRepository = SessionRepository.getInstance(application);
        this.syncRepository = SyncRepository.getInstance(application);

        filteredSessions.addSource(allSessions, sessions -> applyFilter());
        filteredSessions.addSource(searchQuery, query -> applyFilter());

        loadSessions();
    }

    public void loadSessions() {
        sessionRepository.getAllActiveSessions().observeForever(sessions -> {
            allSessions.postValue(sessions != null ? sessions : new ArrayList<>());
        });
    }

    private void applyFilter() {
        List<Session> fullList = allSessions.getValue();
        String query = searchQuery.getValue();

        if (fullList == null) {
            filteredSessions.setValue(new ArrayList<>());
            return;
        }

        if (query == null || query.trim().isEmpty()) {
            filteredSessions.setValue(new ArrayList<>(fullList));
            return;
        }

        String qLower = query.toLowerCase().trim();
        List<Session> filtered = new ArrayList<>();
        for (Session s : fullList) {
            if (s.getTopic() != null && s.getTopic().toLowerCase().contains(qLower)) {
                filtered.add(s);
            }
        }
        filteredSessions.setValue(filtered);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<List<Session>> getAllSessions() {
        return allSessions;
    }

    public LiveData<List<Session>> getFilteredSessions() {
        return filteredSessions;
    }

    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }

    public LiveData<String> getSyncStatusMessage() {
        return syncStatusMessage;
    }

    public void softDeleteSession(String sessionId) {
        sessionRepository.softDeleteSession(sessionId, success -> {
            loadSessions();
        });
    }

    public void syncNow() {
        isSyncing.setValue(true);
        syncRepository.syncPendingSessions((success, count, message) -> {
            isSyncing.postValue(false);
            syncStatusMessage.postValue(message);
            loadSessions();
        });
    }
}
