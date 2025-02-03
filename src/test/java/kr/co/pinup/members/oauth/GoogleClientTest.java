package kr.co.pinup.members.oauth;

public class GoogleClientTest {

//    @Autowired
//    MockMvc mockMvc;
//
//    @MockitoBean
//    private MemberService memberService;
//
//    @MockitoBean
//    private JdbcTemplate jdbcTemplate;
//
//    private MockHttpSession session;
//
//    @BeforeEach
//    void setUp() {
//        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
//        jdbcTemplate = new JdbcTemplate(dataSource);
//
//        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS members (" +
//                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
//                "name VARCHAR(50) NOT NULL," +
//                "email VARCHAR(100) NOT NULL UNIQUE," +
//                "nickname VARCHAR(50) NOT NULL UNIQUE," +
//                "provider_type VARCHAR(50) NOT NULL," +
//                "provider_id VARCHAR(255) NOT NULL," +
//                "role VARCHAR(50) NOT NULL," +
//                "created_at TIMESTAMP," +
//                "updated_at TIMESTAMP" +
//                ")");
//
//        jdbcTemplate.update("INSERT INTO members (name, email, nickname, provider_type, provider_id, role) VALUES (?, ?, ?, ?, ?, ?)",
//                "test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER.toString(), "123456789", MemberRole.ROLE_USER.toString());
//        session = new MockHttpSession();
//    }
//
//    @AfterEach
//    void tearDown() {
//        jdbcTemplate.update("DELETE FROM members");
//        session.clearAttributes();
//        SecurityContextHolder.clearContext();
//    }
}
