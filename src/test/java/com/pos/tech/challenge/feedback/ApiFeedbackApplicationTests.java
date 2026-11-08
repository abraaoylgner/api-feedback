package com.pos.tech.challenge.feedback;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.ses.SesClient;

import javax.sql.DataSource;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.cloud.aws.region.static=us-east-1",
		"spring.cloud.aws.ses.enabled=false",
		"spring.cloud.aws.sqs.enabled=false"
})
class ApiFeedbackApplicationTests {

	@MockitoBean
	private DataSource dataSource;

	@MockitoBean
	private SqsTemplate sqsTemplate;

	@MockitoBean
	private SesClient sesClient;

	@Test
	void contextLoads() {
	}
}