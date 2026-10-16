package com.edumate.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

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
    private final LiveData<List<Session>> allSessions;
    private final LiveData<List<Session>> filteredSessions;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        this.sessionRepository = SessionRepository.getInstance(application);
        this.syncRepository = SyncRepository.getInstance(application);

        this.allSessions = sessionRepository.getAllActiveSessions();
        this.filteredSessions = Transformations.switchMap(searchQuery, query -> {
            MutableLiveData<List<Session>> result = new MutableLiveData<>();
            List<Session> fullList = allSessions.getValue();
            if (fullList == null) {
                result.setValue(new ArrayList<>());
            } else if (query == null || query.trim().isEmpty()) {
                result.setValue(fullList);
            } else {
                String qLower = query.toLowerCase().trim();
                List<Session> filtered = new ArrayList<>();
                for (Session s : fullList) {
                    if (s.getTopic() != null && s.getTopic().toLowerCase().contains(qLower)) {
                        filtered.add(s);
                    }
                }
                result.setValue(filtered);
            }
            return result;
        });
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
            // Refresh list
            searchQuery.setValue(searchQuery.getValue());
        });
    }

    public void syncNow() {
        isSyncing.setValue(true);
        syncRepository.syncPendingSessions((success, count, message) -> {
            isSyncing.postValue(false);
            syncStatusMessage.postValue(message);
            // Refresh sessions
            searchQuery.postValue(searchQuery.getValue());
        });
    }
}
