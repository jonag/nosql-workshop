package nosql.workshop.services;

import com.google.inject.Inject;
import nosql.workshop.model.Installation;
import nosql.workshop.model.stats.CountByActivity;
import org.jongo.MongoCollection;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
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
    // $project:{nbEquipements:{$size:'$equipements'},nom:1,equipements:1}}
    public Installation installationWithMaxEquipments() {
        // TODO codez le service
        return installations.findOne("{$query: {}, $orderby: {equipements.$size: -1}}").as(Installation.class);
    }

    /**
     * Compte le nombre d'installations par activité.
     *
     * @return le nombre d'installations par activité.
     */
    public List<CountByActivity> countByActivity() {
        // TODO codez le service
        throw new UnsupportedOperationException();
    }

    public double averageEquipmentsPerInstallation() {
        // TODO codez le service
        throw new UnsupportedOperationException();
    }

    /**
     * Recherche des installations sportives.
     *
     * @param searchQuery la requête de recherche.
     * @return les résultats correspondant à la requête.
     */
    public List<Installation> search(String searchQuery) {
        Iterable<Installation> iterable = installations.find("{nom: \""+searchQuery+"\"}").as(Installation.class);

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
        // TODO codez le service
        throw new UnsupportedOperationException();
    }
}
