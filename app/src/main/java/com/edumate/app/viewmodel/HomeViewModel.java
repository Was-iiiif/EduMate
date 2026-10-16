package com.edumate.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.edumate.app.model.Session;
import com.edumate.app.repository.SessionRepository;
import com.edumate.app.repository.SyncRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;
    private final SyncRepository syncRepository;

    private final MutableLiveData<SessionRepository.Statistics> statistics = new MutableLiveData<>();
    private final MutableLiveData<String> syncMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.sessionRepository = SessionRepository.getInstance(application);
        this.syncRepository = SyncRepository.getInstance(application);
        loadHomeData();
    }

    public void loadHomeData() {
        sessionRepository.loadStatistics(statistics::postValue);
    }

    public LiveData<List<Session>> getRecentSessions() {
        return sessionRepository.getRecentActiveSessions(5);
    }

    public LiveData<SessionRepository.Statistics> getStatistics() {
        return statistics;
    }

    public LiveData<String> getSyncMessage() {
        return syncMessage;
    }

    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }

    public void syncNow() {
        isSyncing.setValue(true);
        syncRepository.syncPendingSessions((success, count, message) -> {
            isSyncing.postValue(false);
            syncMessage.postValue(message);
            loadHomeData();
        });
    }
}
