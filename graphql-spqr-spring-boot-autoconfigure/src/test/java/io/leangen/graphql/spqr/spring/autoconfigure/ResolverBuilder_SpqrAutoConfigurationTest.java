package io.leangen.graphql.spqr.spring.autoconfigure;

import java.util.Comparator;
import java.util.Scanner;

import graphql.schema.*;
import graphql.schema.diff.DiffSet;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.reporting.CapturingReporter;
import graphql.schema.idl.*;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.spqr.spring.test.ResolverBuilder_TestConfig;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SpqrAutoConfiguration.class, ResolverBuilder_TestConfig.class})
@TestPropertySource(locations = "classpath:application.properties")
public class ResolverBuilder_SpqrAutoConfigurationTest {

    @Autowired
    private SpqrProperties spqrProperties;
    @Autowired
    private GraphQLSchemaGenerator schemaGenerator;
    @Autowired
    private GraphQLSchema schema;

    @Test
    public void propertiesLoad() {
        Assert.assertNotNull(spqrProperties);
        Assert.assertNotNull(spqrProperties.getBasePackages());
        Assert.assertEquals(1, spqrProperties.getBasePackages().length);
        Assert.assertEquals("com.bogus.package", spqrProperties.getBasePackages()[0]);
    }

    @Test
    public void schemaGeneratorConfigTest() {
        Assert.assertNotNull(schemaGenerator);
    }


    @Test
    public void schemaConfigTest() {
        printSchema();

        String expectedSchemaString = new Scanner(ResolverBuilder_SpqrAutoConfigurationTest.class
                .getResourceAsStream("/schema.graphql"), "UTF-8")
                .useDelimiter("\\A")
                .next();

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry reg = schemaParser.parse(expectedSchemaString);
        SchemaGenerator gen = new SchemaGenerator();

        RuntimeWiring.Builder runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .scalar(FileUploadHandler.FILE_UPLOAD_SCALAR);

        GraphQLSchema expected = gen.makeExecutableSchema(reg, runtimeWiring.build());

        diff(expected, schema);
        diff(schema, expected);
    }

    private void printSchema() {
        SchemaPrinter schemaPrinter = new SchemaPrinter(SchemaPrinter.Options.defaultOptions()
                .includeDirectives(false)
                .includeScalarTypes(true)
                .includeExtendedScalarTypes(true)
                .includeSchemaDefintion(true)
                .includeIntrospectionTypes(false)
                .setComparators(DefaultSchemaPrinterComparatorRegistry.newComparators()
                        .addComparator(env -> {
                            env.parentType(GraphQLObjectType.class);
                            env.elementType(GraphQLFieldDefinition.class);
                            return env;
                        }, GraphQLFieldDefinition.class, Comparator.comparing(GraphQLFieldDefinition::getName))
                        .build()));
        System.out.println("Augmented Schema:");
        System.out.println(schemaPrinter.print(schema));
    }

    private void diff(GraphQLSchema augmentedSchema, GraphQLSchema expected) {
        DiffSet diffSet = DiffSet.diffSet(augmentedSchema, expected);
        CapturingReporter capture = new CapturingReporter();
        new SchemaDiff(SchemaDiff.Options.defaultOptions())
                .diffSchema(diffSet, capture);
        Assertions.assertThat(capture.getDangers()).isEmpty();
        Assertions.assertThat(capture.getBreakages()).isEmpty();
    }
}

