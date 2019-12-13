package webserver;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import java.io.BufferedReader;
import java.io.FileReader;

import fisco.*;


@RestController
public class WebController {

	private static Connector c;
	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	WebController() throws Exception{
		c = new Connector();
		c.init();
		c.deployAssetAndRecordAddr();
	}

	@RequestMapping("/register")
	public String register() {
		return ResourceReader.readFile("src/main/front/register.html");
	}

	@RequestMapping("/login")
	public String login() {
		return ResourceReader.readFile("src/main/front/login.html");
	}

	@RequestMapping("/index/{company_name}/{private_key}")
	public String index(@PathVariable("company_name") String company_name, @PathVariable("private_key") String private_key) throws Exception
	{
		String content = ResourceReader.readFile("src/main/front/index.html");
		Cpy cpy = c.login(company_name, private_key);
		content = content.replace("%company_name%", company_name);
		content = content.replace("%company_asset%", String.valueOf(cpy.getAsset_value()));
		content = content.replace("%company_address%", cpy.getCompany_address());
		content = content.replace("%private_key%", private_key);
		return content;
	}

	@RequestMapping("/register/{company_name}/{company_address}/{company_asset}")
	public Cpy doRegister(@PathVariable("company_name") String company_name, @PathVariable("company_address") String company_address, @PathVariable("company_asset") String company_asset) throws Exception
	{
		Cpy cpy = c.register(company_name, company_address, company_asset);
		return cpy;
	}

	@RequestMapping("/login/{company_name}/{private_key}")
	public Cpy doLogin(@PathVariable("company_name") String company_name, @PathVariable("private_key") String private_key) throws Exception
	{
		Cpy cpy = c.login(company_name, private_key);
		return cpy;
	}

	@RequestMapping("/lend/{company_name}/{private_key}/{other_company}/{amount}")
	public Cpy doLend(@PathVariable("company_name") String company_name, @PathVariable("private_key") String private_key, @PathVariable("other_company") String other_company, @PathVariable("amount") String amount) throws Exception
	{
		Cpy cpy = c.login(company_name, private_key);
		if (cpy.getRet_code() == 0) {
			cpy.setRet_code(c.addBil(other_company, company_name, amount));
		}
		return cpy;
	}

	@RequestMapping("/repay/{company_name}/{private_key}/{other_company}/{amount}")
	public Cpy doRepay(@PathVariable("company_name") String company_name, @PathVariable("private_key") String private_key, @PathVariable("other_company") String other_company, @PathVariable("amount") String amount) throws Exception
	{
		Cpy cpy = c.login(company_name, private_key);
		if (cpy.getRet_code() == 0) {
			cpy.setRet_code(c.repayBil(other_company, company_name, amount));
		}
		return cpy;
	}

	@RequestMapping("/finance/{company_name}/{private_key}/{bank_name}/{amount}")
	public Cpy doFinance(@PathVariable("company_name") String company_name, @PathVariable("private_key") String private_key, @PathVariable("bank_name") String bank_name, @PathVariable("amount") String amount) throws Exception
	{
		Cpy cpy = c.login(company_name, private_key);
		if (cpy.getRet_code() == 0) {
			cpy.setRet_code(c.finance(company_name, bank_name, amount));
		}
		return cpy;
	}

	@RequestMapping("/transfer/{company_name}/{private_key}/{credit_company}/{debtor_company}/{amount}")
	public Cpy doTransfer(@PathVariable("company_name") String company_name, @PathVariable("private_key") String private_key, @PathVariable("credit_company") String credit_company, @PathVariable("debtor_company") String debtor_company, @PathVariable("amount") String amount) throws Exception
	{
		Cpy cpy = c.login(company_name, private_key);
		if (cpy.getRet_code() == 0) {
			cpy.setRet_code(c.transferBil(credit_company, company_name, debtor_company, amount));
		}
		return cpy;
	}

	@RequestMapping("/greeting")
	public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
		return new Greeting(counter.incrementAndGet(),
							String.format(template, name));
	}
}
