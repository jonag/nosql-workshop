package nosql.workshop.batch.elasticsearch;

import nosql.workshop.batch.elasticsearch.util.ElasticSearchBatchUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static nosql.workshop.batch.elasticsearch.util.ElasticSearchBatchUtils.dealWithFailures;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Job d'import des rues de towns_paysdeloire.csv vers ElasticSearch (/towns/town)
 */
public class ImportTowns {
    public static void main(String[] args) throws IOException {
        // change the name of the cluster
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "teambs").build();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ImportTowns.class.getResourceAsStream("/csv/towns_paysdeloire.csv")));
             Client elasticSearchClient = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("localhost", 9300))) {


            //checkIndexExists("towns", elasticSearchClient);

            BulkRequestBuilder bulkRequest = elasticSearchClient.prepareBulk();

            reader.lines()
                    .skip(1)
                    .filter(line -> line.length() > 0)
                    .forEach(line -> insertTown(line, bulkRequest, elasticSearchClient));

            BulkResponse bulkItemResponses = bulkRequest.execute().actionGet();

            dealWithFailures(bulkItemResponses);
        }

    }

    private static void insertTown(String line, BulkRequestBuilder bulkRequest, Client elasticSearchClient) {
        line = ElasticSearchBatchUtils.handleComma(line);

        String[] split = line.split(",");

        String townName = split[1].replaceAll("\"", "");
        Double longitude = Double.valueOf(split[6]);
        Double latitude = Double.valueOf(split[7]);

        Double[] location = {longitude, latitude};

        try {
            bulkRequest.add(
                    elasticSearchClient.prepareIndex("towns", "town")
                            .setSource(
                                    jsonBuilder()
                                            .startObject()
                                            .field("townName", townName)
                                            .field("location", location)
                                            .endObject()
                            )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
