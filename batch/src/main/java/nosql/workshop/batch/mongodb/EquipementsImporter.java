package nosql.workshop.batch.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import java.io.*;

public class EquipementsImporter {

    private final DBCollection installationsCollection;

    public EquipementsImporter(DBCollection installationsCollection) {
        this.installationsCollection = installationsCollection;
    }

    public void run() {
        InputStream is = CsvToMongoDb.class.getResourceAsStream("/csv/equipements.csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.lines()
                    .skip(1)
                    .filter(line -> line.length() > 0)
                    .forEach(line -> updateInstallation(line));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void updateInstallation(final String line) {
        String[] columns = line.split(",");

        String installationId = columns[2];

        // create the query
        BasicDBObject queryDBObject = new BasicDBObject();
        queryDBObject.append("_id", installationId);

        // create the new object
        BasicDBObject objectDBObject = new BasicDBObject();
        objectDBObject.append("numero", columns[4]);
        objectDBObject.append("nom", columns[5]);
        objectDBObject.append("type", columns[7]);
        objectDBObject.append("famille", columns[9]);

        // push to the collection
        installationsCollection.getCollection("equipements")
                .update(queryDBObject, objectDBObject);
    }
}
