package com.edumate.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Session;
import com.edumate.app.repository.SessionRepository;

public class FeedbackViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;

    private final MutableLiveData<Session> sessionLiveData = new MutableLiveData<>();
    private final MutableLiveData<Evaluation> evaluationLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public FeedbackViewModel(@NonNull Application application) {
        super(application);
        this.sessionRepository = SessionRepository.getInstance(application);
    }

    public void loadSessionAndEvaluation(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return;

        isLoading.setValue(true);
        sessionRepository.getSessionById(sessionId, session -> {
            sessionLiveData.setValue(session);
            sessionRepository.getEvaluationForSession(sessionId, eval -> {
                isLoading.setValue(false);
                if (eval != null) {
                    evaluationLiveData.setValue(eval);
                } else {
                    errorMessage.setValue("Evaluation details not found for this session.");
                }
            });
        });
    }

    public LiveData<Session> getSession() { return sessionLiveData; }
    public LiveData<Evaluation> getEvaluation() { return evaluationLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}
