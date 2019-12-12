package runner.test;

import java.util.TreeMap;

import org.junit.runner.RunWith;

import com.panther.auth.Auth;
import com.panther.auth.Authentication;
import com.panther.runner.PantherRunner;

@RunWith(PantherRunner.class)
public class RunnerTest {

	@Auth
	public Authentication authHeader() {
		TreeMap<String, String> headers = new TreeMap<String, String>();
		headers.put("Authorization", "Basic cXVldWUtbWFuYWdlcjpxdWV1ZU1hbmFnZXJAMTIzNDU=");
		return () -> headers;
	}
}