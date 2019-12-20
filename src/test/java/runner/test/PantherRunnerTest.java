package runner.test;

import org.junit.runner.RunWith;

import com.panther.auth.Auth;
import com.panther.auth.Authentication;
import com.panther.auth.BasicAuthentication;
import com.panther.runner.PantherRunner;

@RunWith(PantherRunner.class)
public class PantherRunnerTest {

	@Auth
	public Authentication authHeader() {
		BasicAuthentication basicAuth = new BasicAuthentication("queue-manager", "queueManager@12345");
		System.out.println(basicAuth.headers().get("Authorization"));
		return basicAuth;
	}
}