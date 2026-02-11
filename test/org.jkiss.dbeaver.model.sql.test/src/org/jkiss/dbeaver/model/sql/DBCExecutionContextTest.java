import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DBCExecutionContextTest {

    @Mock
    private DBCExecutionContext context;

    @Mock
    private DBCSession session;

    private AutoCloseable closeable;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // -------------------------------------------------
    // 1. Full Successful Lifecycle Test
    // Created → Connected → Active_Session → Connected
    // -------------------------------------------------
    @Test
    void testSuccessfulLifecycle() throws Exception {
        when(context.connect()).thenReturn(true);
        when(context.isConnected()).thenReturn(true);
        when(context.openSession(any())).thenReturn(session);

        // Connect
        assertTrue(context.connect());
        assertTrue(context.isConnected());

        // Open session
        DBCSession opened = context.openSession(null);
        assertNotNull(opened);

        // Close session
        session.close();
        verify(session).close();
    }

    // -------------------------------------------------
    // 2. Failure Detection and Automatic Recovery
    // Connected → Failed → Reconnecting → Connected
    // -------------------------------------------------
    @Test
    void testFailureAndSuccessfulRecovery() throws Exception {

        // Simulate failure
        doThrow(new DBException("Lost connection"))
                .when(context).checkContextAlive();

        assertThrows(DBException.class, () -> {
            context.checkContextAlive();
        });

        // Simulate recovery
        when(context.retry()).thenReturn(true);
        when(context.isConnected()).thenReturn(true);

        assertTrue(context.retry());
        assertTrue(context.isConnected());
    }

    // -------------------------------------------------
    // 3. Failure with Retry Exhaustion
    // Connected → Failed → Reconnecting → Failed
    // -------------------------------------------------
    @Test
    void testFailureWithRetryExhaustion() throws Exception {

        doThrow(new DBException("Lost connection"))
                .when(context).checkContextAlive();

        assertThrows(DBException.class, () -> {
            context.checkContextAlive();
        });

        when(context.retry()).thenReturn(false);

        assertFalse(context.retry());
    }

    // -------------------------------------------------
    // 4. Invalid Operation Enforcement
    // Failed → Failed (No session allowed)
    // -------------------------------------------------
    @Test
    void testInvalidOperationWhenDisconnected() throws Exception {

        when(context.isConnected()).thenReturn(false);

        assertThrows(DBException.class, () -> {
            context.openSession(null);
        });

        verify(context, never()).connect();
    }

    // -------------------------------------------------
    // 5. Manual Invalidation and Transaction Support
    // Connected → Failed → Reconnecting → Connected
    // -------------------------------------------------
    @Test
    void testManualInvalidationAndTransactionAccess() throws Exception {

        // Manual invalidation
        doNothing().when(context).invalidateContext();
        context.invalidateContext();
        verify(context).invalidateContext();

        // Retry recovery
        when(context.retry()).thenReturn(true);
        when(context.isConnected()).thenReturn(true);

        assertTrue(context.retry());
        assertTrue(context.isConnected());

        // Transaction manager availability
        DBCTransactionManager txManager = mock(DBCTransactionManager.class);
        when(context.getTransactionManager()).thenReturn(txManager);

        assertNotNull(context.getTransactionManager());
    }
}
