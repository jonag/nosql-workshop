package nosql.workshop.services;

import com.google.inject.Inject;
import nosql.workshop.model.Installation;
import nosql.workshop.model.stats.Average;
import nosql.workshop.model.stats.CountByActivity;
import org.jongo.MongoCollection;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service permettant de manipuler les installations sportives.
 */
public class InstallationService {

    /**
     * Nom de la collection MongoDB.
     */
    public static final String COLLECTION_NAME = "installations";

    private final MongoCollection installations;

    @Inject
    public InstallationService(MongoDB mongoDB) throws UnknownHostException {
        this.installations = mongoDB.getJongo().getCollection(COLLECTION_NAME);
        createIndexes();
    }

    /**
     * Crée les différents index requis par Mongo
     */
    private void createIndexes() {
        installations.ensureIndex(
                "{"+
                        "nom: \"text\"," +
                        "adresse.commune: \"text\"" +
                "},"+
                "{" +
                    "weights: {" +
                        "nom: 3," +
                        "adresse.commune: 10" +
                    "}," +
                    "default_language: \"french\"" +
                "}"
        );
        installations.ensureIndex("{location: \"2dsphere\"}");
    }

    /**
     * Retourne une installation étant donné son numéro.
     *
     * @param numero le numéro de l'installation.
     * @return l'installation correspondante, ou <code>null</code> si non trouvée.
     */
    public Installation get(String numero) {
        return installations.findOne("{_id: \""+numero+"\"}").as(Installation.class);
    }

    /**
     * Retourne la liste des installations.
     *
     * @param page     la page à retourner.
     * @param pageSize le nombre d'installations par page.
     * @return la liste des installations.
     */
    public List<Installation> list(int page, int pageSize) {
        Iterable<Installation> iterable = installations.find().skip(pageSize*page).limit(pageSize).as(Installation.class);

        List<Installation> installs = new ArrayList<>();
        iterable.forEach(installs::add);

        return installs;
    }

    /**
     * Retourne une installation aléatoirement.
     *
     * @return une installation.
     */
    public Installation random() {
        long count = count();
        int random = new Random().nextInt((int) count);
        return this.list(0, (int) count).get(random);
    }

    /**
     * Retourne le nombre total d'installations.
     *
     * @return le nombre total d'installations
     */
    public long count() {
        return installations.count();
    }

    /**
     * Retourne l'installation avec le plus d'équipements.
     *
     * @return l'installation avec le plus d'équipements.
     */
    public Installation installationWithMaxEquipments() {
        Iterable<Installation> iterable = installations.aggregate(
                "{" +
                    "$project: {" +
                        "numberOfEquipements: {$size: \"$equipements\"}," +
                        "nom: 1," +
                        "equipements: 1" +
                    "}," +
                "}"
            ).and(
                "{" +
                    "$sort: {" +
                        "numberOfEquipements: -1" +
                    "}" +
                "}"
            ).and(
                "{" +
                    "$limit : 1" +
                "}"
            ).as(Installation.class);

        return iterable.iterator().next();
    }

    /**
     * Compte le nombre d'installations par activité.
     *
     * @return le nombre d'installations par activité.
     */
    public List<CountByActivity> countByActivity() {
        Iterable<CountByActivity> iterable = installations.aggregate(
            "{" +
                "$unwind: \"$equipements\"" +
            "},"
        ).and(
                "{" +
                        "$unwind: \"$equipements.activites\"" +
                        "},"
        ).and(
            "{" +
                "$group: {" +
                    "_id: \"$equipements.activites\"," +
                    "total: {$sum: 1}" +
                "}" +
            "}"
        ).and(
            "{" +
                "$project : {" +
                    "activite: \"$_id\"," +
                    "total: 1" +
                "}" +
            "}"
        ).as(CountByActivity.class);

        List<CountByActivity> countByActivities = new ArrayList<>();
        iterable.forEach(countByActivities::add);

        return countByActivities;
    }

    /*
     * see http://docs.mongodb.org/manual/reference/operator/aggregation/group/#group-by-null
     */
    public double averageEquipmentsPerInstallation() {
        Iterable<Average> average = installations.aggregate(
                "{\n" +
                        "    $group : {\n" +
                        "       _id : null,\n" +
                        "       average: { $avg: {$size : \"$equipements\" }}\n" +
                        "    }\n" +
                        "  }"
        ).as(Average.class);
        return average.iterator().next().getAverage();
    }

    /**
     * Recherche des installations sportives.
     *
     * @param searchQuery la requête de recherche.
     * @return les résultats correspondant à la requête.
     */
    public List<Installation> search(String searchQuery) {
        Iterable<Installation> iterable = installations.find(
                "{$text: {$search: #}},"+
                "{score: {\"$meta\": \"textScore\"}},"
            , searchQuery)
            //.sort("{score: {\"$meta\": \"textScore\"}}")
            .as(Installation.class);

        List<Installation> installs = new ArrayList<>();
        iterable.forEach(installs::add);

        return installs;
    }

    /**
     * Recherche des installations sportives par proximité géographique.
     *
     * @param lat      latitude du point de départ.
     * @param lng      longitude du point de départ.
     * @param distance rayon de recherche.
     * @return les installations dans la zone géographique demandée.
     */
    public List<Installation> geosearch(double lat, double lng, double distance) {
        Iterable<Installation> iterable = installations.find("{location: { $near :{ $geometry :{ type : \"Point\" , coordinates : ["+lng+", "+lat+"]}, $maxDistance : "+distance+"}} }").as(Installation.class);

        List<Installation> installs = new ArrayList<>();
        iterable.forEach(installs::add);

        return installs;
    }
}
