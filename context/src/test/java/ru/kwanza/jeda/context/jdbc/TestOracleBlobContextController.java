package ru.kwanza.jeda.context.jdbc;

/**
 * @author Dmitry Zagorovsky
 */
public class TestOracleBlobContextController extends AbstractJDBCBlobContextControllerTest {

    @Override
    protected String getContextFileName() {
        return "oracle-blob-context-controller-test-config.xml";
    }

}