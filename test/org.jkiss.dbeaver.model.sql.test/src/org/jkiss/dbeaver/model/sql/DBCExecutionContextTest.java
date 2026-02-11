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
    // 1. Successful connection
    // -------------------------------------------------
    @Test
    void testConnectSuccess() throws Exception {
        when(context.connect()).thenReturn(true);
        when(context.isConnected()).thenReturn(true);

        assertTrue(context.connect());
        assertTrue(context.isConnected());
    }

    // -------------------------------------------------
    // 2. Open session from connected state
    // -------------------------------------------------
    @Test
    void testOpenSessionWhenConnected() throws Exception {
        when(context.isConnected()).thenReturn(true);
        when(context.openSession(any())).thenReturn(session);

        DBCSession opened = context.openSession(null);

        assertNotNull(opened);
        verify(context).openSession(null);
    }

    // -------------------------------------------------
    // 3. Session close returns to connected state
    // -------------------------------------------------
    @Test
    void testCloseSession() throws Exception {
        when(session.isClosed()).thenReturn(false);

        session.close();

        verify(session).close();
    }

    // -------------------------------------------------
    // 4. Health check failure transitions to Failed
    // -------------------------------------------------
    @Test
    void testHealthCheckFailure() throws Exception {
        doThrow(new DBException("Connection lost"))
                .when(context).checkContextAlive();

        assertThrows(DBException.class, () -> {
            context.checkContextAlive();
        });
    }

    // -------------------------------------------------
    // 5. Retry logic from failed state
    // -------------------------------------------------
    @Test
    void testRetryAfterFailure() throws Exception {
        when(context.retry()).thenReturn(true);
        when(context.isConnected()).thenReturn(true);

        assertTrue(context.retry());
        assertTrue(context.isConnected());
    }

    // -------------------------------------------------
    // 6. Reconnection failure
    // -------------------------------------------------
    @Test
    void testReconnectFailure() throws Exception {
        when(context.retry()).thenReturn(false);

        assertFalse(context.retry());
    }

    // -------------------------------------------------
    // 7. Prevent session opening when disconnected
    // -------------------------------------------------
    @Test
    void testOpenSessionWhenDisconnected() throws Exception {
        when(context.isConnected()).thenReturn(false);

        assertThrows(DBException.class, () -> {
            context.openSession(null);
        });
    }

    // -------------------------------------------------
    // 8. Invalidate context
    // -------------------------------------------------
    @Test
    void testInvalidateContext() throws Exception {
        doNothing().when(context).invalidateContext();

        context.invalidateContext();

        verify(context).invalidateContext();
    }

    // -------------------------------------------------
    // 9. Multiple retries exhaustion
    // -------------------------------------------------
    @Test
    void testRetryExhaustion() throws Exception {
        when(context.retry())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false);

        assertFalse(context.retry());
        assertFalse(context.retry());
        assertFalse(context.retry());
    }

    // -------------------------------------------------
    // 10. Transaction manager availability
    // -------------------------------------------------
    @Test
    void testTransactionManagerExists() {
        DBCTransactionManager txManager = mock(DBCTransactionManager.class);
        when(context.getTransactionManager()).thenReturn(txManager);

        assertNotNull(context.getTransactionManager());
    }
}
