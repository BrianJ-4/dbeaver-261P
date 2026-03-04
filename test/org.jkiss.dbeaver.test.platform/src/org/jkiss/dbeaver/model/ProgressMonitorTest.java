package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ProgressMonitorTest extends DBeaverUnitTest {

    @Test
    public void testTaskReportingBehavior() {
        // Create Mock of Progress Monitor
        DBRProgressMonitor mockMonitor = mock(DBRProgressMonitor.class);

        // Simulate process that should report progress
        mockMonitor.beginTask("Testing Mocking", 100);
        mockMonitor.worked(50);
        mockMonitor.done();

        // Verify if code tells monitor it was done
        verify(mockMonitor).beginTask("Testing Mocking", 100);
        verify(mockMonitor).worked(50);
        verify(mockMonitor).done();
    }
}