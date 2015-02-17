package nosql.workshop.batch.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import java.io.*;

public class ActivitesImporter {

    private final DBCollection installationsCollection;

    public ActivitesImporter(DBCollection installationsCollection) {
        this.installationsCollection = installationsCollection;
    }

    public void run() {
        InputStream is = CsvToMongoDb.class.getResourceAsStream("/csv/activites.csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.lines()
                    .skip(1)
                    .filter(line -> line.length() > 0)
                    .forEach(line -> updateEquipement(line));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void updateEquipement(final String line) {
        String[] columns = line
                .substring(1, line.length() - 1)
                .split("\",\"");

        // Programmation défensive : certaines lignes n'ont pas d'activités de définies
        if (columns.length >= 6) {
            String equipementId = columns[2].trim();

            // create the query
            BasicDBObject queryDBObject = new BasicDBObject("numero", equipementId);

            // create the new object
            BasicDBObject activiteDBObject = new BasicDBObject("activites", columns[5]);
            
            // push the new object to the list
            BasicDBObject updateDBObject = new BasicDBObject("$push", activiteDBObject);

            // update to the collection
            installationsCollection.update(queryDBObject, updateDBObject);
        }
    }
}
