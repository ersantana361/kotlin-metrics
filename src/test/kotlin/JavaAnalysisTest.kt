import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import java.io.File

class JavaAnalysisTest {
    
    @Test
    fun `should calculate LCOM for Java class with field usage`() {
        val javaCode = """
            public class UserService {
                private String name;
                private int age;
                
                public String getName() {
                    return name.toUpperCase();
                }
                
                public void setName(String name) {
                    this.name = name;
                }
                
                public int getAge() {
                    return age;
                }
                
                public void setAge(int age) {
                    this.age = age;
                }
            }
        """.trimIndent()
        
        val analysis = analyzeJavaClass(javaCode)
        
        // getName and setName share 'name' field
        // getAge and setAge share 'age' field  
        // No cross-sharing between name and age methods
        assertEquals(2, analysis.lcom) // 4 non-sharing pairs - 2 sharing pairs
        assertEquals(4, analysis.methodCount)
        assertEquals(2, analysis.propertyCount)
    }
    
    @Test
    fun `should detect Java Entity pattern with JPA annotations`() {
        val javaCode = """
            import javax.persistence.*;
            
            @Entity
            @Table(name = "users")
            public class User {
                @Id
                private Long id;
                private String name;
                private String email;
                
                public Long getId() {
                    return id;
                }
                
                public void setId(Long id) {
                    this.id = id;
                }
                
                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    User user = (User) obj;
                    return Objects.equals(id, user.id);
                }
                
                @Override
                public int hashCode() {
                    return Objects.hash(id);
                }
            }
        """.trimIndent()
        
        val classDecl = parseJavaClass(javaCode)
        val entityAnalysis = analyzeJavaEntity(classDecl, "User.java")
        
        assertTrue(entityAnalysis.confidence > 0.8) // High confidence for entity
        assertTrue(entityAnalysis.hasUniqueId)
        assertTrue(entityAnalysis.isMutable) // Has setters
        assertEquals(listOf("id"), entityAnalysis.idFields)
    }
    
    @Test
    fun `should detect Java Value Object pattern`() {
        val javaCode = """
            public final class Money {
                private final BigDecimal amount;
                private final String currency;
                
                public Money(BigDecimal amount, String currency) {
                    this.amount = amount;
                    this.currency = currency;
                }
                
                public BigDecimal getAmount() {
                    return amount;
                }
                
                public String getCurrency() {
                    return currency;
                }
                
                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    Money money = (Money) obj;
                    return Objects.equals(amount, money.amount) && 
                           Objects.equals(currency, money.currency);
                }
                
                @Override
                public int hashCode() {
                    return Objects.hash(amount, currency);
                }
            }
        """.trimIndent()
        
        val classDecl = parseJavaClass(javaCode)
        val valueObjectAnalysis = analyzeJavaValueObject(classDecl, "Money.java")
        
        assertTrue(valueObjectAnalysis.confidence > 0.6)
        assertTrue(valueObjectAnalysis.isImmutable) // All final fields
        assertTrue(valueObjectAnalysis.hasValueEquality) // Has equals/hashCode
        assertEquals(listOf("amount", "currency"), valueObjectAnalysis.properties)
    }
    
    @Test
    fun `should detect Java Service pattern with Spring annotations`() {
        val javaCode = """
            import org.springframework.stereotype.Service;
            import org.springframework.beans.factory.annotation.Autowired;
            
            @Service
            public class UserService {
                @Autowired
                private final UserRepository userRepository;
                
                public UserService(UserRepository userRepository) {
                    this.userRepository = userRepository;
                }
                
                public User createUser(String name, String email) {
                    if (name == null || email == null) {
                        throw new IllegalArgumentException("Name and email required");
                    }
                    
                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    
                    return userRepository.save(user);
                }
                
                public List<User> findActiveUsers() {
                    return userRepository.findByStatus("ACTIVE");
                }
            }
        """.trimIndent()
        
        val classDecl = parseJavaClass(javaCode)
        val serviceAnalysis = analyzeJavaService(classDecl, "UserService.java")
        
        assertTrue(serviceAnalysis.confidence > 0.7)
        assertTrue(serviceAnalysis.isStateless) // Only has final dependencies
        assertTrue(serviceAnalysis.hasDomainLogic) // Has business methods
        assertTrue(serviceAnalysis.methods.contains("createUser"))
        assertTrue(serviceAnalysis.methods.contains("findActiveUsers"))
    }
    
    @Test
    fun `should calculate cyclomatic complexity for Java methods`() {
        val javaCode = """
            public class ComplexService {
                public String processData(String input, int type) {
                    if (input == null) {  // +1
                        return "error";
                    }
                    
                    switch (type) {      // +3 (3 cases)
                        case 1:
                            return input.toUpperCase();
                        case 2:
                            return input.toLowerCase();
                        default:
                            return input;
                    }
                }
                
                public boolean validate(Object obj) {
                    return obj != null && obj.toString().length() > 0; // +2 (&&, base +1)
                }
            }
        """.trimIndent()
        
        val analysis = analyzeJavaClass(javaCode)
        
        // processData: base(1) + if(1) + switch(3) = 5
        // validate: base(1) + &&(1) = 2
        val complexityAnalysis = analysis.complexity
        assertEquals(2, complexityAnalysis.methods.size)
        
        val processDataComplexity = complexityAnalysis.methods.find { it.methodName == "processData" }
        val validateComplexity = complexityAnalysis.methods.find { it.methodName == "validate" }
        
        assertNotNull(processDataComplexity)
        assertNotNull(validateComplexity)
        assertEquals(5, processDataComplexity!!.cyclomaticComplexity)
        assertEquals(2, validateComplexity!!.cyclomaticComplexity)
        
        assertEquals(7, complexityAnalysis.totalComplexity)
        assertEquals(3.5, complexityAnalysis.averageComplexity, 0.1)
        assertEquals(5, complexityAnalysis.maxComplexity)
    }
    
    @Test
    fun `should generate appropriate suggestions for Java classes`() {
        val javaCode = """
            public class ProblematicClass {
                private String field1;
                private String field2;
                private String field3;
                private String field4;
                private String field5;
                
                // Many methods that don't share fields
                public void method1() { field1 = "1"; }
                public void method2() { field2 = "2"; }
                public void method3() { field3 = "3"; }
                public void method4() { field4 = "4"; }
                public void method5() { field5 = "5"; }
                
                // Complex method
                public String complexMethod(int x) {
                    if (x > 0) {
                        if (x > 10) {
                            if (x > 100) {
                                return "large";
                            } else {
                                return "medium";
                            }
                        } else {
                            return "small";
                        }
                    } else {
                        return "negative";
                    }
                }
            }
        """.trimIndent()
        
        val analysis = analyzeJavaClass(javaCode)
        
        assertTrue(analysis.lcom > 5) // Poor cohesion
        assertTrue(analysis.complexity.maxComplexity > 3) // Complex method
        
        val suggestions = analysis.suggestions
        assertTrue(suggestions.any { it.message.contains("Consider splitting") })
        assertTrue(suggestions.any { it.message.contains("complexity") })
    }
    
    private fun analyzeJavaClass(javaCode: String): ClassAnalysis {
        val classDecl = parseJavaClass(javaCode)
        return analyzeJavaClass(classDecl, "Test.java")
    }
    
    private fun parseJavaClass(javaCode: String): ClassOrInterfaceDeclaration {
        val cu = StaticJavaParser.parse(javaCode)
        return cu.findAll(ClassOrInterfaceDeclaration::class.java).first()
    }
}