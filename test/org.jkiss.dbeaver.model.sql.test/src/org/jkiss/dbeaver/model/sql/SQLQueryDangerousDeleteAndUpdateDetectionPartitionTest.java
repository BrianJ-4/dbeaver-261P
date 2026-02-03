package org.jkiss.dbeaver.model.sql;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SQLQueryDangerousDeleteAndUpdateDetectionPartitionTest extends DBeaverUnitTest {

    // Testing isDeleteUpdateDangerous()
    
    @Test
    public void P1_UnparseableSQLShouldBeNotDangerous() {
        var query = new SQLQuery(null, "THIS IS NOT SQL");
        assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void P2_NonDeleteUpdateStatementShouldBeNotDangerous() {
        var query = new SQLQuery(null, "SELECT * FROM table");
        assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void P3_DeleteWithoutWhereShouldBeDangerous() {
        var query = new SQLQuery(null, "DELETE FROM table");
        assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void P4_DeleteWithWhereShouldBeNotDangerous() {
        var query = new SQLQuery(null, "DELETE FROM table WHERE id = 100");
        assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void P5_UpdateWithoutWhereShouldBeDangerous() {
        var query = new SQLQuery(null, "UPDATE table SET a = 1");
        assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void P6_UpdateWithWhereShouldBeNotDangerous() {
        var query = new SQLQuery(null, "UPDATE table SET a = 1 WHERE id = 100");
        assertFalse(query.isDeleteUpdateDangerous());
    }
}
