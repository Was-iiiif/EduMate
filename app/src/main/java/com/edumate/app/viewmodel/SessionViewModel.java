package com.edumate.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.edumate.app.background.SyncWorker;
import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Message;
import com.edumate.app.model.Persona;
import com.edumate.app.model.Session;
import com.edumate.app.repository.AIRepository;
import com.edumate.app.repository.SessionRepository;

import java.util.ArrayList;
import java.util.List;

public class SessionViewModel extends AndroidViewModel {
    public static final int MAX_TURNS = 5;

    private final SessionRepository sessionRepository;
    private final AIRepository aiRepository;

    private final MutableLiveData<Session> sessionLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isEvaluating = new MutableLiveData<>(false);
    private final MutableLiveData<Evaluation> evaluationResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentTurn = new MutableLiveData<>(1);
    private final MutableLiveData<String> latestStudentQuestion = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isTtsSpeaking = new MutableLiveData<>(false);

    private boolean isInitialized = false;

    public SessionViewModel(@NonNull Application application) {
        super(application);
        this.sessionRepository = SessionRepository.getInstance(application);
        this.aiRepository = AIRepository.getInstance(application);
    }

    public void initSession(String topic, Persona persona, String existingSessionId) {
        if (isInitialized) return;
        isInitialized = true;

        if (existingSessionId != null && !existingSessionId.isEmpty()) {
            sessionRepository.getSessionById(existingSessionId, session -> {
                if (session != null) {
                    sessionLiveData.setValue(session);
                    sessionRepository.getMessagesForSessionSync(session.getId(), msgs -> {
                        messagesLiveData.setValue(msgs != null ? msgs : new ArrayList<>());
                        calculateTurnCount(msgs);
                    });
                }
            });
            return;
        }

        // Create fresh new session
        Session session = new Session();
        session.setTopic(topic);
        session.setPersona(persona != null ? persona.name() : Persona.BEGINNER.name());
        sessionRepository.insertSession(session, id -> {
            sessionLiveData.setValue(session);
            // Trigger student opening question
            triggerStudentQuestion(session, new ArrayList<>());
        });
    }

    public void sendLearnerExplanation(String text) {
        Session session = sessionLiveData.getValue();
        if (session == null || text == null || text.trim().isEmpty()) return;

        Message learnerMsg = new Message(session.getId(), Message.SENDER_LEARNER, text.trim());
        sessionRepository.insertMessage(learnerMsg, id -> {
            List<Message> currentList = messagesLiveData.getValue();
            if (currentList == null) currentList = new ArrayList<>();
            currentList.add(learnerMsg);
            messagesLiveData.setValue(new ArrayList<>(currentList));

            calculateTurnCount(currentList);

            // Fetch AI student reply or prompt evaluation if turn limit reached
            int turns = currentTurn.getValue() != null ? currentTurn.getValue() : 1;
            if (turns >= MAX_TURNS) {
                // Auto trigger evaluation or prompt user
                evaluateSession();
            } else {
                triggerStudentQuestion(session, currentList);
            }
        });
    }

    private void triggerStudentQuestion(Session session, List<Message> history) {
        isLoading.setValue(true);
        Persona p;
        try {
            p = Persona.valueOf(session.getPersona());
        } catch (Exception e) {
            p = Persona.BEGINNER;
        }

        aiRepository.generateStudentReply(session.getTopic(), p, history, new AIRepository.StudentReplyCallback() {
            @Override
            public void onSuccess(String studentReply) {
                isLoading.setValue(false);
                Message studentMsg = new Message(session.getId(), Message.SENDER_STUDENT, studentReply);
                sessionRepository.insertMessage(studentMsg, id -> {
                    List<Message> currentList = messagesLiveData.getValue();
                    if (currentList == null) currentList = new ArrayList<>();
                    currentList.add(studentMsg);
                    messagesLiveData.setValue(new ArrayList<>(currentList));
                    latestStudentQuestion.setValue(studentReply);
                });
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void evaluateSession() {
        Session session = sessionLiveData.getValue();
        List<Message> currentList = messagesLiveData.getValue();
        if (session == null || currentList == null || currentList.isEmpty()) {
            errorMessage.setValue("No content to evaluate.");
            return;
        }

        isEvaluating.setValue(true);
        aiRepository.evaluateSession(session.getTopic(), currentList, new AIRepository.EvaluationCallback() {
            @Override
            public void onSuccess(Evaluation eval) {
                isEvaluating.setValue(false);
                // Complete session in transactional SQLite write
                sessionRepository.completeSessionWithEvaluation(session, eval, success -> {
                    if (success) {
                        evaluationResult.setValue(eval);
                        // Trigger WorkManager background sync attempt
                        SyncWorker.enqueueSyncWork(getApplication());
                    } else {
                        errorMessage.setValue("Failed to record evaluation in local database.");
                    }
                });
            }

            @Override
            public void onError(String error) {
                isEvaluating.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    private void calculateTurnCount(List<Message> msgs) {
        if (msgs == null) return;
        int count = 0;
        for (Message m : msgs) {
            if (m.isFromLearner()) count++;
        }
        currentTurn.setValue(Math.min(MAX_TURNS, count + 1));
    }

    public LiveData<Session> getSession() { return sessionLiveData; }
    public LiveData<List<Message>> getMessages() { return messagesLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsEvaluating() { return isEvaluating; }
    public LiveData<Evaluation> getEvaluationResult() { return evaluationResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Integer> getCurrentTurn() { return currentTurn; }
    public LiveData<String> getLatestStudentQuestion() { return latestStudentQuestion; }
    public LiveData<Boolean> getIsTtsSpeaking() { return isTtsSpeaking; }

    public void setTtsSpeaking(boolean speaking) {
        isTtsSpeaking.setValue(speaking);
    }
}
