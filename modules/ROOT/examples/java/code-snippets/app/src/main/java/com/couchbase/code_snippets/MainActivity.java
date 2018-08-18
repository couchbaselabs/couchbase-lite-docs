package com.couchbase.code_snippets;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.couchbase.lite.ArrayFunction;
import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.Blob;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.EncryptionKey;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Expression;
import com.couchbase.lite.FullTextExpression;
import com.couchbase.lite.FullTextIndexItem;
import com.couchbase.lite.Function;
import com.couchbase.lite.IndexBuilder;
import com.couchbase.lite.Join;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.MessageEndpoint;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Ordering;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.URLEndpoint;
import com.couchbase.lite.ValueIndexItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


import static com.couchbase.lite.CBLError.Code.CBLErrorBusy;

public class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.getSimpleName();
    protected final static String DATABASE_NAME = "database";
    Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //@Test
    public void testGettingStarted() throws CouchbaseLiteException, URISyntaxException {
        // tag::getting-started[]

        // Get the database (and create it if it doesn’t exist).
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        Database database = new Database("mydb", config);

        // Create a new document (i.e. a record) in the database.
        MutableDocument mutableDoc = new MutableDocument()
            .setFloat("version", 2.0F)
            .setString("type", "SDK");

        // Save it to the database.
        database.save(mutableDoc);

        // Update a document.
        mutableDoc = database.getDocument(mutableDoc.getId()).toMutable();
        mutableDoc.setString("language", "Java");
        database.save(mutableDoc);
        Document document = database.getDocument(mutableDoc.getId());
        // Log the document ID (generated by the database) and properties
        Log.i(TAG, "Document ID :: " + document.getId());
        Log.i(TAG, "Learning " + document.getString("language"));

        // Create a query to fetch documents of type SDK.
        Query query = QueryBuilder.select(SelectResult.all())
            .from(DataSource.database(database))
            .where(Expression.property("type").equalTo(Expression.string("SDK")));
        ResultSet result = query.execute();
        Log.i(TAG, "Number of rows ::  " + result.allResults().size());

        // Create replicators to push and pull changes to and from the cloud.
        Endpoint targetEndpoint = new URLEndpoint(new URI("ws://localhost:4984/example_sg_db"));
        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(database, targetEndpoint);
        replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);

        // Add authentication.
        replConfig.setAuthenticator(new BasicAuthenticator("john", "pass"));

        // Create replicator.
        Replicator replicator = new Replicator(replConfig);

        // Listen to replicator change events.
        replicator.addChangeListener(new ReplicatorChangeListener() {
            @Override
            public void changed(ReplicatorChange change) {
                if (change.getStatus().getError() != null)
                    Log.i(TAG, "Error code ::  " + change.getStatus().getError().getCode());
            }
        });

        // Start replication.
        replicator.start();

        // end::getting-started[]

        database.delete();
    }

    public void test1xAttachments() throws CouchbaseLiteException, IOException {
        // if db exist, delete it
        deleteDB("android-sqlite", getApplicationContext().getFilesDir());

        ZipUtils.unzip(getAsset("replacedb/android140-sqlite.cblite2.zip"), getApplicationContext().getFilesDir());

        Database db = new Database("android-sqlite", new DatabaseConfiguration(getApplicationContext()));
        try {

            Document doc = db.getDocument("doc1");

            // For Validation
            Dictionary attachments = doc.getDictionary("_attachments");
            Blob blob = attachments.getBlob("attach1");
            byte[] content = blob.getContent();
            // For Validation

            byte[] attach = String.format(Locale.ENGLISH, "attach1").getBytes();
            Arrays.equals(attach, content);

        } finally {
            // close db
            db.close();
            // if db exist, delete it
            deleteDB("android-sqlite", getApplicationContext().getFilesDir());
        }

        Document document = new MutableDocument();

        // tag::1x-attachment[]
        Dictionary attachments = document.getDictionary("_attachments");
        Blob blob = attachments != null ? attachments.getBlob("avatar") : null;
        byte[] content = blob != null ? blob.getContent() : null;
        // end::1x-attachment[]
    }

    // ### New Database
    public void testNewDatabase() throws CouchbaseLiteException {
        // tag::new-database[]
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        Database database = new Database("my-database", config);
        // end::new-database[]

        database.delete();
    }

    // ### Database Encryption
    public void testDatabaseEncryption() throws CouchbaseLiteException {
        // tag::database-encryption[]
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        config.setEncryptionKey(new EncryptionKey("PASSWORD"));
        Database database = new Database("mydb", config);
        // end::database-encryption[]
    }

    // ### Logging
    public void testLogging() throws CouchbaseLiteException {
        // tag::logging[]
        Database.setLogLevel(LogDomain.REPLICATOR, LogLevel.VERBOSE);
        Database.setLogLevel(LogDomain.QUERY, LogLevel.VERBOSE);
        // end::logging[]
    }

    // ### Loading a pre-built database
    public void testPreBuiltDatabase() throws IOException {
        // tag::prebuilt-database[]
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        ZipUtils.unzip(getAsset("replacedb/android200-sqlite.cblite2.zip"), getApplicationContext().getFilesDir());
        File path = new File(getApplicationContext().getFilesDir(), "android-sqlite");
        try {
            Database.copy(path, "travel-sample", config);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Could not load pre-built database");
        }
        // end::prebuilt-database[]
    }


    // helper methods

    // if db exist, delete it
    private void deleteDB(String name, File dir) throws CouchbaseLiteException {
        // database exist, delete it
        if (Database.exists(name, )) {
            // sometimes, db is still in used, wait for a while. Maximum 3 sec
            for (int i = 0; i < 10; i++) {
                try {
                    Database.delete(name, dir);
                    break;
                } catch (CouchbaseLiteException ex) {
                    if (ex.getCode() == CBLErrorBusy) {
                        try {
                            Thread.sleep(300);
                        } catch (Exception e) {
                        }
                    } else {
                        throw ex;
                    }
                }
            }
        }
    }

    // ### Initializers
    public void testInitializers() {
        // tag::initializer[]
        MutableDocument newTask = new MutableDocument();
        newTask.setString("type", "task");
        newTask.setString("owner", "todo");
        newTask.setDate("createdAt", new Date());
        try {
            database.save(newTask);
        } catch (CouchbaseLiteException e) {
            com.couchbase.lite.internal.support.Log.e(TAG, e.toString());
        }
        // end::initializer[]
    }

    // ### Mutability
    public void testMutability() {
        try {
            database.save(new MutableDocument("xyz"));
        } catch (CouchbaseLiteException e) {
        }

        // tag::update-document[]
        Document document = database.getDocument("xyz");
        MutableDocument mutableDocument = document.toMutable();
        mutableDocument.setString("name", "apples");
        try {
            database.save(mutableDocument);
        } catch (CouchbaseLiteException e) {
            com.couchbase.lite.internal.support.Log.e(TAG, e.toString());
        }
        // end::update-document[]
    }

    // ### Typed Accessors
    public void testTypedAccessors() {
        MutableDocument newTask = new MutableDocument();

        // tag::date-getter[]
        newTask.setValue("createdAt", new Date());
        Date date = newTask.getDate("createdAt");
        // end::date-getter[]
    }

    // ### Batch operations
    public void testBatchOperations() {
        // tag::batch[]
        try {
            database.inBatch(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        MutableDocument doc = new MutableDocument();
                        doc.setValue("type", "user");
                        doc.setValue("name", String.format("user %d", i));
                        doc.setBoolean("admin", false);
                        try {
                            database.save(doc);
                        } catch (CouchbaseLiteException e) {
                            com.couchbase.lite.internal.support.Log.e(TAG, e.toString());
                        }
                        com.couchbase.lite.internal.support.Log.i(TAG, String.format("saved user document %s", doc.getString("name")));
                    }
                }
            });
        } catch (CouchbaseLiteException e) {
            com.couchbase.lite.internal.support.Log.e(TAG, e.toString());
        }
        // end::batch[]
    }

    // ### Blobs
    public void testBlobs() {
        MutableDocument newTask = new MutableDocument();

        // tag::blob[]
        InputStream is = getAsset("avatar.jpg");
        try {
            Blob blob = new Blob("image/jpeg", is);
            newTask.setBlob("avatar", blob);
            database.save(newTask);

            Blob taskBlob = newTask.getBlob("avatar");
            byte[] bytes = taskBlob.getContent();
        } catch (CouchbaseLiteException e) {
            com.couchbase.lite.internal.support.Log.e(TAG, e.toString());
        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }
        }
        // end::blob[]
    }

    // ### Indexing
    public void testIndexing() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-index[]
            database.createIndex("TypeNameIndex",
                IndexBuilder.valueIndex(ValueIndexItem.property("type"),
                    ValueIndexItem.property("name")));
            // end::query-index[]
        }
    }

    // ### SELECT statement
    public void testSelectStatement() throws CouchbaseLiteException {
        {
            // tag::query-select-meta[]
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                    SelectResult.property("name"),
                    SelectResult.property("type"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("hotel")))
                .orderBy(Ordering.expression(Meta.id));

            try {
                ResultSet rs = query.execute();
                for (Result result : rs) {
                    Log.i("Sample", String.format("hotel id -> %s", result.getString("id")));
                    Log.i("Sample", String.format("hotel name -> %s", result.getString("name")));
                }
            } catch (CouchbaseLiteException e) {
                Log.e("Sample", e.getLocalizedMessage());
            }
            // end::query-select-meta[]
        }
    }

    // META function
    public void testMetaFunction() throws CouchbaseLiteException {
        // For Documentation
        {
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("airport")))
                .orderBy(Ordering.expression(Meta.id));
            ResultSet rs = query.execute();
            for (Result result : rs) {
                Log.w("Sample", String.format("airport id -> %s", result.getString("id")));
                Log.w("Sample", String.format("airport id -> %s", result.getString(0)));
            }
        }
    }

    // ### all(*)
    public void testSelectAll() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-select-all[]
            Query query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("hotel")));
            // end::query-select-all[]
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample", String.format("hotel -> %s", result.getDictionary(DATABASE_NAME).toMap()));
        }
    }

    // ###　WHERE statement
    public void testWhereStatement() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-where[]
            Query query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("hotel")))
                .limit(Expression.intValue(10));
            ResultSet rs = query.execute();
            for (Result result : rs) {
                Dictionary all = result.getDictionary(DATABASE_NAME);
                Log.i("Sample", String.format("name -> %s", all.getString("name")));
                Log.i("Sample", String.format("type -> %s", all.getString("type")));
            }
            // end::query-where[]
        }
    }

    // ####　Collection Operators
    public void testCollectionStatement() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-collection-operator-contains[]
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                    SelectResult.property("name"),
                    SelectResult.property("public_likes"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("hotel"))
                    .and(ArrayFunction.contains(Expression.property("public_likes"), Expression.string("Armani Langworth"))));
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample", String.format("public_likes -> %s", result.getArray("public_likes").toList()));
            // end::query-collection-operator-contains[]
        }
    }

    // IN operator
    public void testInOperator() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-collection-operator-in[]
            Expression[] values = new Expression[] {
                Expression.property("first"),
                Expression.property("last"),
                Expression.property("username")
            };

            Query query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.database(database))
                .where(Expression.string("Armani").in(values));
            // end::query-collection-operator-in[]

            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.w("Sample", String.format("%s", result.toMap().toString()));
        }
    }

    // Pattern Matching
    public void testPatternMatching() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-like-operator[]
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                    SelectResult.property("country"),
                    SelectResult.property("name"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("landmark"))
                    .and(Expression.property("name").like(Expression.string("Royal Engineers Museum"))));
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample", String.format("name -> %s", result.getString("name")));
            // end::query-like-operator[]
        }
    }

    // ### Wildcard Match
    public void testWildcardMatch() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-like-operator-wildcard-match[]
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                    SelectResult.property("country"),
                    SelectResult.property("name"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("landmark"))
                    .and(Expression.property("name").like(Expression.string("Eng%e%"))));
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample", String.format("name -> %s", result.getString("name")));
            // end::query-like-operator-wildcard-match[]
        }
    }

    // Wildcard Character Match
    public void testWildCharacterMatch() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-like-operator-wildcard-character-match[]
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                    SelectResult.property("country"),
                    SelectResult.property("name"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("landmark"))
                    .and(Expression.property("name").like(Expression.string("Eng____r"))));
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample", String.format("name -> %s", result.getString("name")));
            // end::query-like-operator-wildcard-character-match[]
        }
    }

    // ### Regex Match
    public void testRegexMatch() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-regex-operator[]
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                    SelectResult.property("country"),
                    SelectResult.property("name"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("landmark"))
                    .and(Expression.property("name").regex(Expression.string("\\bEng.*r\\b"))));
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample", String.format("name -> %s", result.getString("name")));
            // end::query-regex-operator[]
        }
    }

    // JOIN statement
    public void testJoinStatement() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-join[]
            Query query = QueryBuilder.select(
                SelectResult.expression(Expression.property("name").from("airline")),
                SelectResult.expression(Expression.property("callsign").from("airline")),
                SelectResult.expression(Expression.property("destinationairport").from("route")),
                SelectResult.expression(Expression.property("stops").from("route")),
                SelectResult.expression(Expression.property("airline").from("route")))
                .from(DataSource.database(database).as("airline"))
                .join(Join.join(DataSource.database(database).as("route"))
                    .on(Meta.id.from("airline").equalTo(Expression.property("airlineid").from("route"))))
                .where(Expression.property("type").from("route").equalTo(Expression.string("route"))
                    .and(Expression.property("type").from("airline").equalTo(Expression.string("airline")))
                    .and(Expression.property("sourceairport").from("route").equalTo(Expression.string("RIX"))));
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.w("Sample", String.format("%s", result.toMap().toString()));
            // end::query-join[]
        }
    }

    // ### GROUPBY statement
    public void testGroupByStatement() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-groupby[]
            Query query = QueryBuilder.select(
                SelectResult.expression(Function.count(Expression.string("*"))),
                SelectResult.property("country"),
                SelectResult.property("tz"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("airport"))
                    .and(Expression.property("geo.alt").greaterThanOrEqualTo(Expression.intValue(300))))
                .groupBy(Expression.property("country"),
                    Expression.property("tz"))
                .orderBy(Ordering.expression(Function.count(Expression.string("*"))).descending());
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample",
                    String.format("There are %d airports on the %s timezone located in %s and above 300ft",
                        result.getInt("$1"),
                        result.getString("tz"),
                        result.getString("country")));
            // end::query-groupby[]
        }
    }

    // ### ORDER BY statement
    public void testOrderByStatement() throws CouchbaseLiteException {
        // For Documentation
        {
            // tag::query-orderby[]
            Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                    SelectResult.property("name"))
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("hotel")))
                .orderBy(Ordering.property("name").ascending())
                .limit(Expression.intValue(10));
            ResultSet rs = query.execute();
            for (Result result : rs)
                Log.i("Sample", String.format("%s", result.toMap()));
            // end::query-orderby[]
        }
    }

    void prepareIndex() throws CouchbaseLiteException {
        // tag::fts-index[]
        database.createIndex("nameFTSIndex", IndexBuilder.fullTextIndex(FullTextIndexItem.property("name")).ignoreAccents(false));
        // end::fts-index[]
    }

    public void testFTS() throws CouchbaseLiteException {
        // tag::fts-query[]
        Expression whereClause = FullTextExpression.index("nameFTSIndex").match("buy");
        Query ftsQuery = QueryBuilder.select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .where(whereClause);
        ResultSet ftsQueryResult = ftsQuery.execute();
        for (Result result : ftsQueryResult)
            com.couchbase.lite.internal.support.Log.i(TAG, String.format("document properties %s", result.getString(0)));
        // end::fts-query[]
    }

    /* The `tag::replication[]` example is inlined in java.adoc */

    public void testTroubleshooting() {
        // tag::replication-logging[]
        Database.setLogLevel(LogDomain.REPLICATOR, LogLevel.VERBOSE);
        // end::replication-logging[]
    }

    public void testReplicationStatus() throws URISyntaxException {
        URI uri = new URI("ws://localhost:4984/db");
        Endpoint endpoint = new URLEndpoint(uri);
        ReplicatorConfiguration config = new ReplicatorConfiguration(database, endpoint);
        config.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PULL);
        Replicator replication = new Replicator(config);

        // tag::replication-status[]
        replication.addChangeListener(new ReplicatorChangeListener() {
            @Override
            public void changed(ReplicatorChange change) {
                if (change.getStatus().getActivityLevel() == Replicator.ActivityLevel.STOPPED)
                    com.couchbase.lite.internal.support.Log.i(TAG, "Replication stopped");
            }
        });
        // end::replication-status[]
    }

    public void testHandlingNetworkErrors() throws URISyntaxException {
        URI uri = new URI("ws://localhost:4984/db");
        Endpoint endpoint = new URLEndpoint(uri);
        ReplicatorConfiguration config = new ReplicatorConfiguration(database, endpoint);
        config.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PULL);
        Replicator replication = new Replicator(config);

        // tag::replication-error-handling[]
        replication.addChangeListener(new ReplicatorChangeListener() {
            @Override
            public void changed(ReplicatorChange change) {
                CouchbaseLiteException error = change.getStatus().getError();
                if (error != null)
                    com.couchbase.lite.internal.support.Log.w(TAG, "Error code:: %d", error.getCode());
            }
        });
        replication.start();
        // end::replication-error-handling[]

        replication.stop();
    }

    public void testReplicationCustomHeader() throws URISyntaxException {
        URI uri = new URI("ws://localhost:4984/db");
        Endpoint endpoint = new URLEndpoint(uri);

        // tag::replication-custom-header[]
        ReplicatorConfiguration config = new ReplicatorConfiguration(database, endpoint);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("CustomHeaderName", "Value");
        config.setHeaders(headers);
        // end::replication-custom-header[]
    }

    // ### Certificate Pinning

    public void testCertificatePinning() throws URISyntaxException, IOException {
        URI uri = new URI("ws://localhost:4984/db");
        Endpoint endpoint = new URLEndpoint(uri);
        ReplicatorConfiguration config = new ReplicatorConfiguration(database, endpoint);

        // tag::certificate-pinning[]
        InputStream is = getAsset("cert.cer");
        byte[] cert = IOUtils.toByteArray(is);
        is.close();
        config.setPinnedServerCertificate(cert);
        // end::certificate-pinning[]
    }

    // ### Reset replicator checkpoint
    public void testReplicationResetCheckpoint() throws URISyntaxException {
        URI uri = new URI("ws://localhost:4984/db");
        Endpoint endpoint = new URLEndpoint(uri);
        ReplicatorConfiguration config = new ReplicatorConfiguration(database, endpoint);
        config.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PULL);
        Replicator replicator = new Replicator(config);
        replicator.start();

        // tag::replication-reset-checkpoint[]
        replicator.resetCheckpoint();
        replicator.start();
        // end::replication-reset-checkpoint[]

        replicator.stop();
    }

}
