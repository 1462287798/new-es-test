package com.example.elasticsearh;


import com.example.elasticsearh.service.ContentService;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ElasticsearchApplicationTests {

    public static final String INDEX_NAME = "spring-data";
    @Autowired
    ContentService contentService;

    @Autowired
    RestHighLevelClient highLevelClient;
    //ES的Java客户端，类似于JDBC中的Connection

    //用highLevelClient调用indices方法获得这个ES中所有和索引相关的客户端IndicesClient
    //GET _cat/indices可以用于查询所有索引

    @Test
    void test(){
        try {
            contentService.searchPageHighlightBuilder("java",1,30).forEach(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void testCreateNewIndex() throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(INDEX_NAME);
        //准备好了一个即将被操作的索引，索引名是INDEX_NAME，即spring-data
        IndicesClient indicesClient = highLevelClient.indices();//indices方法获得了一个用来操作索引的客户端
        CreateIndexResponse createIndexResponse = indicesClient.create(
                createIndexRequest, RequestOptions.DEFAULT);
        //用indicesClient对象调用create方法建立一个索引，并得到CreateIndexResponse表示建立后的响应
        //这个响应类似于：
        /*
        {
            "acknowledged" : true,
            "shards_acknowledged" : true,
            "index" : "spring-data"
        }
        */
        assertEquals(createIndexResponse.index(), INDEX_NAME);
        //assertEquals断言相等
        assertTrue(createIndexResponse.isAcknowledged());
        //assertEquals断言结果为true
    }




    @Test
    void testDeleteIndex() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest(INDEX_NAME);
        IndicesClient indicesClient = highLevelClient.indices();
        AcknowledgedResponse acknowledgedResponse = indicesClient.delete(request, RequestOptions.DEFAULT);
        assertTrue(acknowledgedResponse.isAcknowledged());
    }

    @Test
    void testExistsIndex() throws IOException {
        IndicesClient indicesClient = highLevelClient.indices();
        GetIndexRequest indexRequest = new GetIndexRequest(INDEX_NAME);
        boolean exists = indicesClient.exists(indexRequest, RequestOptions.DEFAULT);
        assertTrue(exists);
        //这段代码无需获得索引响应，直接利用IndicesClient就可以判断索引是否存在
    }

    @Test
    void testGetIndex() throws IOException {
        IndicesClient indicesClient = highLevelClient.indices();
        GetIndexRequest indexRequest = new GetIndexRequest(INDEX_NAME);
        GetIndexResponse getIndexResponse =
                indicesClient.get(indexRequest, RequestOptions.DEFAULT);
        String[] indices = getIndexResponse.getIndices();
        assertEquals(indices[0], INDEX_NAME);
        //真正的取出GetIndexResponse索引响应，再用这个GetIndexResponse进行操作
    }

    @Test
    void testGetAllIndices() throws IOException {
        IndicesClient indicesClient = highLevelClient.indices();

        GetAliasesRequest getAliasesRequest = new GetAliasesRequest();
        GetAliasesResponse aliasesResponse =
                indicesClient.getAlias(getAliasesRequest, RequestOptions.DEFAULT);

        Map<String, Set<AliasMetadata>> indices = aliasesResponse.getAliases();
        indices.keySet().forEach(k->{
            System.out.print(k + ": ");
            indices.get(k).forEach(System.out::println);
            System.out.println();
        });
        //indices的键放的就是所有索引的名字
        //值Set<AliasMetadata>放的是与这个索引相关的元数据（这个的所以的描述信息）
    }
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void testAddDocument() throws IOException {
        Map<String, Integer> users = new HashMap<>();
        users.put("Tom", 14);
        users.put("Jerry", 2);
        users.put("Ben", 18);
        // {"Tom":14, "Jerry":2, "Ben": 18}

        IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                .id("10")
                .source(users);
        //此处只是准备好了一个文档，并没有真正建立

        IndexResponse response =
                highLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        assertEquals(response.getResult().getLowercase(), "created");
    }

    @Test
    void testIsExists() throws IOException {
        GetRequest request = new GetRequest(INDEX_NAME, "10");
        boolean exists = highLevelClient.exists(request, RequestOptions.DEFAULT);
        assertTrue(exists);
    }

    @Test
    void testGetDocument() throws IOException {
        GetRequest getRequest = new GetRequest(INDEX_NAME, "10");
        GetResponse response = highLevelClient.get(getRequest, RequestOptions.DEFAULT);
        System.out.println(response.getSourceAsString());
    }

    @Test
    void testUpdateDocument() throws IOException {
        UpdateRequest updateRequest =
                new UpdateRequest(INDEX_NAME, "10")
                        .doc("tom", 2);
        UpdateResponse updateResponse
                = highLevelClient.update(updateRequest, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocument() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest(INDEX_NAME, "10");
        DeleteResponse deleteResponse
                = highLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        System.out.println(deleteResponse);
    }

    @Test
    void testBulkRequest() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("10s");

        for (int i = 0; i < 100; i++)
            bulkRequest.add(new IndexRequest(INDEX_NAME)
                    .id(String.valueOf(i))
                    .source("tom", i));
        //vlkRequest.add(
                // new UpdateRequest(
                // INDEX_NAME, String.valueOf(i)).doc("tom", i+1));
//            bulkRequest.add(
//                    new DeleteRequest(
//                            INDEX_NAME, String.valueOf(i)));

                BulkResponse bulkResponse = highLevelClient
                        .bulk(bulkRequest, RequestOptions.DEFAULT);
        assertTrue(!bulkResponse.hasFailures());
    }


}
