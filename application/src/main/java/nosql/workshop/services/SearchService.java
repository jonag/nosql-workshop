package nosql.workshop.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.QueryBuilder;
import nosql.workshop.model.Installation;
import nosql.workshop.model.suggest.TownSuggest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by Chris on 12/02/15.
 */
public class SearchService {
    public static final String INSTALLATIONS_INDEX = "installations";
    public static final String INSTALLATION_TYPE = "installation";
    public static final String TOWNS_INDEX = "towns";
    public static final String ES_HOST = "es.host";
    public static final String ES_TRANSPORT_PORT = "es.transport.port";
    public static final String KEY_LOCATION = "location";
    public static final String FIELD_TOWN_NAME = "townName";
    public static final Double[] CARQUEFOU_COORD = new Double[]{-1.49181,47.2975};
    private static final String TOWN_TYPE = "town";
    final Client elasticSearchClient;
    final ObjectMapper objectMapper;

    @Inject
    public SearchService(@Named(ES_HOST) String host, @Named(ES_TRANSPORT_PORT) int transportPort) {
        // change the name of the cluster
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "teambs").build();
        elasticSearchClient = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(host, transportPort));

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Recherche les installations à l'aide d'une requête full-text
     * @param searchQuery la requête
     * @return la listes de installations
     */
    public List<Installation> search(String searchQuery) {
        System.out.println("search "+ searchQuery);


        SearchResponse response = elasticSearchClient.prepareSearch(INSTALLATIONS_INDEX)
                .setTypes(INSTALLATION_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.queryString("search " + searchQuery))
                .execute()
                .actionGet();

        SearchHit[] searchHits = response.getHits().getHits();

        List<Installation> installations = new ArrayList<>();

        if (searchHits.length > 0) {
            for (SearchHit searchHit : searchHits) {
                try {
                    installations.add(objectMapper.readValue(searchHit.getSourceAsString(), Installation.class));
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            }
        }

        return installations;
    }

    /**
     * Transforme un résultat de recherche ES en objet installation.
     *
     * @param searchHit l'objet ES.
     * @return l'installation.
     */
    private Installation mapToInstallation(SearchHit searchHit) {
        try {
            return objectMapper.readValue(searchHit.getSourceAsString(), Installation.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TownSuggest> suggestTownName(String townName){
        // create the response
        SuggestResponse suggestResponse =
                elasticSearchClient.prepareSuggest(TOWNS_INDEX).addSuggestion(
                        new CompletionSuggestionBuilder("town_suggest")
                                .field(FIELD_TOWN_NAME)
                                .text(townName)
                ).execute().actionGet();

        // get the suggestion
        CompletionSuggestion compSuggestion = suggestResponse.getSuggest().getSuggestion("town_suggest");

        // map the option into the list of town suggestion
        List<TownSuggest> townSuggests = new ArrayList<>();
        List<CompletionSuggestion.Entry> entryList = compSuggestion.getEntries();
        for (CompletionSuggestion.Entry.Option option : entryList.get(0).getOptions()) {
            String townNameOption = option.getText().string();
            townSuggests.add(new TownSuggest(townNameOption, Arrays.asList(getTownLocation(townNameOption))));
        }

        return townSuggests;
    }

    public Double[] getTownLocation(String townName) {
        Double[] coordinates = new Double[2];

        SearchResponse response = elasticSearchClient.prepareSearch(TOWNS_INDEX)
                .setTypes(TOWN_TYPE)
                .setSearchType(SearchType.QUERY_AND_FETCH)
                .setQuery(QueryBuilders.queryString(FIELD_TOWN_NAME + ":" + townName)) // Query
                .execute()
                .actionGet();
        SearchHit[] searchHits = response.getHits().getHits();

        if(searchHits.length>0){
            Map<String,Object> result = searchHits[0].getSource();
            ArrayList<Double> location = (ArrayList<Double>) result.get(KEY_LOCATION);
            coordinates[0]= location.get(0);
            coordinates[1]= location.get(1);
        }else{
            //Si la recherche ne correspond à aucune ville
            coordinates = CARQUEFOU_COORD;
        }
        return coordinates;
    }


}
