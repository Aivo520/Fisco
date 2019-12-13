package fisco;

import java.math.BigInteger;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.EncryptType;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tuples.generated.Tuple3;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class Connector {
	
	private Web3j web3j;
	private Credentials credentials;
	private String contractAddress;

	public Web3j getWeb3j() {
		return web3j;
	}

	public void setWeb3j(Web3j web3j) {
		this.web3j = web3j;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public String getContractAddress() {
		return contractAddress;
	}

	public void setContractAddress(String contractAddress) {
		this.contractAddress = contractAddress;
	}
	
	public void init() throws Exception {
		@SuppressWarnings("resource")
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		Service service = context.getBean(Service.class);
		service.run();

		ChannelEthereumService channelEthereumService = new ChannelEthereumService();
		channelEthereumService.setChannelService(service);
		web3j = Web3j.build(channelEthereumService, 1);
		
		credentials = GenCredential.create(Keys.createEcKeyPair());
	}
	
	private static BigInteger gasPrice = new BigInteger("30000000");
	private static BigInteger gasLimit = new BigInteger("30000000");

	public void deployAssetAndRecordAddr() {
		try {
			Company company = Company.deploy(web3j, credentials, new StaticGasProvider(gasPrice, gasLimit)).send();
			contractAddress = company.getContractAddress();
			System.out.println(" deploy Asset success, contract address is " + contractAddress);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println(" deploy Asset contract failed, error message is  " + e.getMessage());
		}
	}
	
	public Cpy register(String company_name, String company_address, String asset_value) throws Exception
	{
		Cpy ret = new Cpy();
		ret.setCompany_address(company_address);
		ret.setCompany_name(company_name);
		ret.setAsset_value(Integer.valueOf(asset_value));
		
		Company company = Company.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
		
		TransactionReceipt receipt = company.hasCompany(company_name).send();
		BigInteger big = company.getHasCompanyOutput(receipt).getValue1();
		System.out.println("register: " + big.intValue());
		
		if(big.compareTo(new BigInteger("0")) == 0) 
		{
			ret.setRet_code(0);
			EncryptType.encryptType = 0;
			Credentials credentials2 = GenCredential.create();
			ret.setAccount(credentials2.getAddress());
			ret.setPrivate_key(credentials2.getEcKeyPair().getPrivateKey().toString(16));
			ret.setPublic_key(credentials2.getEcKeyPair().getPublicKey().toString(16));
			
			receipt = company.register(company_name, company_address, 
					new BigInteger(asset_value), ret.getAccount(), ret.getPrivate_key(), ret.getPublic_key()).send();
			big = company.getRegisterOutput(receipt).getValue1();
		}
		else {
			ret.setRet_code(big.intValue());
		}
		return ret;
	}
	
	public Cpy login(String company_name, String privateKey) throws Exception
	{
		Cpy ret = new Cpy();
		ret.setCompany_name(company_name);
		ret.setPrivate_key(privateKey);
		
		Company company = Company.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));

		TransactionReceipt receipt = company.canLogin(company_name, privateKey).send();
		BigInteger big = company.getHasCompanyOutput(receipt).getValue1();
		System.out.println("login: " + big.intValue());
		
		if (big.compareTo(new BigInteger("0")) == 0)
		{
			ret.setRet_code(0);
			receipt = company.login(company_name).send();
			Tuple3<BigInteger, String, String> tuple = company.getLoginOutput(receipt);
			ret.setAsset_value(tuple.getValue1().intValue());
			ret.setCompany_address(tuple.getValue2());
			ret.setPrivate_key(tuple.getValue3());
		}
		else {
			ret.setRet_code(big.intValue());
		}
		
		return ret;
	}
	
	public int addBil(String creditor, String debtor, String amount) throws Exception
	{
		Company company = Company.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
		
		TransactionReceipt receipt = company.addBil(creditor, debtor, new BigInteger(amount)).send();
		BigInteger big = company.getAddBilOutput(receipt).getValue1();
		System.out.println("addBil: " + big.intValue());
		if (big.compareTo(new BigInteger("0")) == 0)
		{
			return 0;
		}
		else {
			return big.intValue();
		}
	}
	
	public int repayBil(String creditor, String debtor, String amount) throws Exception
	{
		Company company = Company.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
		
		TransactionReceipt receipt = company.repayBil(creditor, debtor, new BigInteger(amount)).send();
		BigInteger big = company.getRepayBilOutput(receipt).getValue1();
		System.out.println("repayBil: " + big.intValue());
		if (big.compareTo(new BigInteger("0")) == 0)
		{
			return 0;
		}
		else {
			return big.intValue();
		}
	}
	
	public int transferBil(String new_creditor, String creditor, String debtor, String amount) throws Exception
	{
		Company company = Company.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
		System.out.println(new_creditor + creditor + debtor + amount);
		TransactionReceipt receipt = company.transferBil(new_creditor, creditor, debtor, new BigInteger(amount)).send();
		BigInteger big = company.getTransferBilOutput(receipt).getValue1();
		System.out.println("transferBil: " + big.intValue());
		
		if (big.compareTo(new BigInteger("0")) == 0) {
			return 0;
		}
		else {
			return big.intValue();
		}
	}
	
	public int finance(String company_name, String bank_name, String amount) throws Exception
	{
		Company company = Company.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
		
		TransactionReceipt receipt = company.finance(company_name, bank_name, new BigInteger(amount)).send();
		BigInteger big = company.getFinanceOutput(receipt).getValue1();
		System.out.println("finance: " + big.intValue());
		if (big.compareTo(new BigInteger("0")) == 0)
		{
			return 0;
		}
		else {
			return big.intValue();
		}
	}
	
}
