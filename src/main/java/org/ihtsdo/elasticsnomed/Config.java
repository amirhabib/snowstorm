package org.ihtsdo.elasticsnomed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vanroy.springdata.jest.JestElasticsearchTemplate;
import com.github.vanroy.springdata.jest.mapper.DefaultJestResultsMapper;
import io.kaicode.elasticvc.api.BranchService;
import io.kaicode.elasticvc.api.VersionControlHelper;
import io.kaicode.rest.util.branchpathrewrite.BranchPathUriRewriteFilter;
import io.searchbox.client.JestClient;
import org.ihtsdo.elasticsnomed.core.data.domain.Concept;
import org.ihtsdo.elasticsnomed.core.data.domain.Description;
import org.ihtsdo.elasticsnomed.core.data.domain.Relationship;
import org.ihtsdo.elasticsnomed.core.data.repositories.config.ConceptStoreMixIn;
import org.ihtsdo.elasticsnomed.core.data.repositories.config.DescriptionStoreMixIn;
import org.ihtsdo.elasticsnomed.core.data.repositories.config.RelationshipStoreMixIn;
import org.ihtsdo.elasticsnomed.core.data.services.ReferenceSetTypesConfigurationService;
import org.ihtsdo.elasticsnomed.core.rf2.rf2import.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

@SpringBootApplication(
		exclude = {ElasticsearchAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class,
				ContextStackAutoConfiguration.class})
@EnableElasticsearchRepositories(
		basePackages = {
				"org.ihtsdo.elasticsnomed.core.data.repositories",
				"io.kaicode.elasticvc.repositories"
		})
@EnableConfigurationProperties
public class Config {

	@Autowired
	private JestClient jestClient;

	public static final PageRequest PAGE_OF_ONE = new PageRequest(0, 1);

	@Bean
	public ExecutorService taskExecutor() {
		return Executors.newCachedThreadPool();
	}

	@Bean
	public JestElasticsearchTemplate elasticsearchTemplate() throws UnknownHostException {
		final ObjectMapper elasticSearchMapper = Jackson2ObjectMapperBuilder
				.json()
				.defaultViewInclusion(false)
				.failOnUnknownProperties(false)
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.mixIn(Concept.class, ConceptStoreMixIn.class)
				.mixIn(Description.class, DescriptionStoreMixIn.class)
				.mixIn(Relationship.class, RelationshipStoreMixIn.class)
				.build();

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();

		return new JestElasticsearchTemplate(
				jestClient,
				new MappingElasticsearchConverter(mappingContext),
				new DefaultJestResultsMapper(mappingContext, new EntityMapper() {
					@Override
					public String mapToString(Object o) throws IOException {
						return elasticSearchMapper.writeValueAsString(o);
					}
					@Override
					public <T> T mapToObject(String s, Class<T> aClass) throws IOException {
						return elasticSearchMapper.readValue(s, aClass);
					}
				})
		);
	}

	@Bean
	public ObjectMapper getGeneralMapper() {
		return Jackson2ObjectMapperBuilder
				.json()
				.defaultViewInclusion(false)
				.failOnUnknownProperties(false)
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.build();
	}

	@Bean
	public BranchService getBranchService() {
		return new BranchService();
	}

	@Bean
	public ImportService getImportService() {
		return new ImportService();
	}

	@Bean
	public VersionControlHelper getVersionControlHelper() {
		return new VersionControlHelper();
	}

	@Bean
	@ConfigurationProperties(prefix = "refset")
	public ReferenceSetTypesConfigurationService getReferenceSetTypesService() {
		return new ReferenceSetTypesConfigurationService();
	}

	@Bean
	public FilterRegistrationBean getUrlRewriteFilter() {
		// Encode branch paths in uri to allow request mapping to work
		return new FilterRegistrationBean(new BranchPathUriRewriteFilter(
				"/branches/(.*)/children",
				"/branches/(.*)/parents",
				"/branches/(.*)/actions/.*",
				"/branches/(.*)",
				"/rebuild/(.*)",
				"/browser/(.*)/concepts.*",
				"/browser/(.*)/descriptions.*",
				"/(.*)/concepts/.*",
				"/(.*)/members.*",
				"/mrcm/(.*)/domain-attributes",
				"/mrcm/(.*)/attribute-values.*",
				"/browser/(.*)/validate/concept"
		));
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(not(regex("/error")))
				.build();
	}

}