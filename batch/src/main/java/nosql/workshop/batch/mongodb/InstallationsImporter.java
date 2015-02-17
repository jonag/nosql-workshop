package nosql.workshop.batch.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.elasticsearch.index.analysis.CharMatcher;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Importe les 'installations' dans MongoDB.
 */
public class InstallationsImporter {

    private final DBCollection installationsCollection;

    public InstallationsImporter(DBCollection installationsCollection) {
        this.installationsCollection = installationsCollection;
    }

    public void run() {
        InputStream is = CsvToMongoDb.class.getResourceAsStream("/csv/installations.csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.lines()
                    .skip(1)
                    .filter(line -> line.length() > 0)
                    .forEach(line -> installationsCollection.save(toDbObject(line)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DBObject toDbObject(final String line) {
        String[] columns = line
                .substring(1, line.length() - 1)
                .split("\",\"");

        BasicDBObject installation = new BasicDBObject();

        installation.append("_id", columns[1]);
        installation.append("nom", columns[0]);

        BasicDBObject adresse = new BasicDBObject();
        adresse.append("numero", columns[6]);
        adresse.append("voie", columns[7]);
        adresse.append("lieuDit", columns[5]);
        adresse.append("codePostal", columns[4]);
        adresse.append("commune", columns[2]);
        installation.append("adresse", adresse);

        BasicDBObject location = new BasicDBObject();
        location.append("type", "Point");
        location.append("coordinates", columns[8].substring(1, columns[8].length() -1).split(","));
        installation.append("location", location);

        installation.append("multiCommune", columns[16].equals("Oui"));
        installation.append("nbPlacesParking", columns[17]);
        installation.append("nbPlacesParkingHandicapes", columns[18]);
        if (columns.length >= 29) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = format.parse(columns[28]);
                installation.append("dateMiseAJourFiche", date);
            } catch (ParseException e) {
                System.out.println("Date au mauvais format (ignorée) : " + columns[28]);
            }
        }

        return installation;
    }
}
