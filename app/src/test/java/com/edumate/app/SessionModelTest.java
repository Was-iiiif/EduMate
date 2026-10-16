package com.edumate.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.edumate.app.model.Persona;
import com.edumate.app.model.Session;

import org.junit.Test;

public class SessionModelTest {

    @Test
    public void testNewSession_hasValidDefaults() {
        Session s = new Session();
        assertNotNull(s.getId());
        assertTrue(s.getId().length() > 10);
        assertEquals(Session.STATUS_IN_PROGRESS, s.getStatus());
        assertEquals(Session.SYNC_PENDING, s.getSyncStatus());
        assertFalse(s.isDeleted());
        assertEquals(Persona.BEGINNER.name(), s.getPersona());
    }

    @Test
    public void testSoftDelete_setsDeletedAtAndFlag() {
        Session s = new Session();
        assertFalse(s.isDeleted());

        long now = System.currentTimeMillis();
        s.setDeletedAt(now);

        assertTrue(s.isDeleted());
        assertEquals(Long.valueOf(now), s.getDeletedAt());
    }

    @Test
    public void testSyncStatusTransitions() {
        Session s = new Session();
        assertEquals(Session.SYNC_PENDING, s.getSyncStatus());

        s.setSyncStatus(Session.SYNC_SYNCED);
        assertEquals(Session.SYNC_SYNCED, s.getSyncStatus());
    }
}
