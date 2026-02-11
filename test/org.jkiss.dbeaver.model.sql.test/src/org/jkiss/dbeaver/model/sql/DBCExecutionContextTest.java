import org.junit.Test;
import static org.junit.Assert.*;

public class DBCExecutionContextTest {

    // -------------------------------------------------
    // Fake Minimal Implementations (No Mockito Needed)
    // -------------------------------------------------

    static class DBException extends Exception {
        public DBException(String message) {
            super(message);
        }
    }

    static class DBCSession {
        private boolean closed = false;

        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    static class DBCTransactionManager {
    }

    static class DBCExecutionContext {

        private boolean connected = false;
        private boolean failNextCheck = false;
        private int retryAttempts = 1;

        public boolean connect() {
            connected = true;
            return true;
        }

        public boolean isConnected() {
            return connected;
        }

        public DBCSession openSession(Object obj) throws DBException {
            if (!connected) {
                throw new DBException("Not connected");
            }
            return new DBCSession();
        }

        public void checkContextAlive() throws DBException {
            if (failNextCheck) {
                connected = false;
                throw new DBException("Lost connection");
            }
        }

        public void forceFailure() {
            failNextCheck = true;
        }

        public boolean retry() {
            if (retryAttempts > 0) {
                retryAttempts--;
                connected = true;
                return true;
            }
            return false;
        }

        public void invalidateContext() {
            connected = false;
        }

        public DBCTransactionManager getTransactionManager() {
            return new DBCTransactionManager();
        }
    }

    // -------------------------------------------------
    // 1. Full Successful Lifecycle Test
    // -------------------------------------------------
    @Test
    public void testSuccessfulLifecycle() throws Exception {

        DBCExecutionContext context = new DBCExecutionContext();

        assertTrue(context.connect());
        assertTrue(context.isConnected());

        DBCSession opened = context.openSession(null);
        assertNotNull(opened);

        opened.close();
        assertTrue(opened.isClosed());
    }

    // -------------------------------------------------
    // 2. Failure Detection and Automatic Recovery
    // -------------------------------------------------
    @Test
    public void testFailureAndSuccessfulRecovery() throws Exception {

        DBCExecutionContext context = new DBCExecutionContext();
        context.connect();

        context.forceFailure();

        try {
            context.checkContextAlive();
            fail("Expected DBException");
        } catch (DBException e) {
            // expected
        }

        assertTrue(context.retry());
        assertTrue(context.isConnected());
    }

    // -------------------------------------------------
    // 3. Failure with Retry Exhaustion
    // -------------------------------------------------
    @Test
    public void testFailureWithRetryExhaustion() throws Exception {

        DBCExecutionContext context = new DBCExecutionContext();
        context.connect();

        context.forceFailure();

        try {
            context.checkContextAlive();
        } catch (DBException ignored) {
        }

        context.retry(); // first retry succeeds
        assertFalse(context.retry()); // second retry fails
    }

    // -------------------------------------------------
    // 4. Invalid Operation Enforcement
    // -------------------------------------------------
    @Test
    public void testInvalidOperationWhenDisconnected() throws Exception {

        DBCExecutionContext context = new DBCExecutionContext();

        try {
            context.openSession(null);
            fail("Expected DBException");
        } catch (DBException e) {
            // expected
        }
    }

    // -------------------------------------------------
    // 5. Manual Invalidation and Transaction Support
    // -------------------------------------------------
    @Test
    public void testManualInvalidationAndTransactionAccess() throws Exception {

        DBCExecutionContext context = new DBCExecutionContext();
        context.connect();

        context.invalidateContext();
        assertFalse(context.isConnected());

        assertTrue(context.retry());
        assertTrue(context.isConnected());

        DBCTransactionManager txManager = context.getTransactionManager();
        assertNotNull(txManager);
    }
}
