/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.sql;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SQLQueryDangerousIsDropDangerousTest extends DBeaverUnitTest {

    @Test
    public void noDropStatementShouldReturnFalse() {
        var query = new SQLQuery(null, "SELECT * FROM table WHERE id = ?");
        assertFalse(query.isDropDangerous());
    }

    @Test
    public void dropTableStatementShouldBeDangerous() {
        var query = new SQLQuery(null, "DROP table users");
        assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropSchemaStatementShouldBeDangerous() {
        var query = new SQLQuery(null, "DROP schema users");
        assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropStatementWithMixedCaseShouldBeDangerous() {
        var query = new SQLQuery(null, "DrOp TaBlE users");
        assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropTableIfExistsShouldBeDangerous() {
        var query = new SQLQuery(null, "DROP TABLE IF EXISTS users");
        assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropViewStatementShouldBeDangerous() {
        var query = new SQLQuery(null, "DROP VIEW active_users");
        assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropIndexStatementShouldBeDangerous() {
        var query = new SQLQuery(null, "DROP INDEX user_index");
        assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropKeywordInCommentShouldNotBeDangerous() {
        var query = new SQLQuery(
            null,
            "-- DROP TABLE users\nSELECT * FROM users"
        );
        assertFalse(query.isDropDangerous());
    }

    @Test
    public void dropKeywordInsideStringLiteralShouldNotBeDangerous() {
        var query = new SQLQuery(
            null,
            "SELECT 'DROP TABLE users' AS message"
        );
        assertFalse(query.isDropDangerous());
    }

    @Test
    public void emptyQueryShouldNotBeDangerous() {
        var query = new SQLQuery(null, "   ");
        assertFalse(query.isDropDangerous());
    }

    @Test
    public void createTableStatementShouldNotBeDropDangerous() {
        var query = new SQLQuery(
            null,
            "CREATE TABLE users (id INT)"
        );
        assertFalse(query.isDropDangerous());
    }
}
