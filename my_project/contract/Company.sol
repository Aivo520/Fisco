pragma solidity ^0.4.24;

import "./Table.sol";

contract Company {
	constructor() public {
        createTable();
    }
	
	function createTable() private {
        TableFactory tf = TableFactory(0x1001);
        tf.createTable("t_company", "company_name", "account, private_key, public_key, company_address, asset_value");
		tf.createTable("t_bil", "creditor", "debtor, amount");
    }
	
	function openCompanyTable() private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_company");
        return table;
    }
	
	function openBilTable() private returns(Table) {
		TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_bil");
        return table;
	}
	
	function hasCompany(string company_name) public returns(int256) {
		Table table = openCompanyTable();
		Entries entries = table.select(company_name, table.newCondition());
		if (0 == uint256(entries.size())) {
			return 0;
		}
		else{
			return 1;
		}
    }
	
	function register(string company_name, string company_address, int256 asset_value, string account, string private_key, string public_key) public returns(int256) {
		if (hasCompany(company_name) == 1) {
			return -1;
		}
		
		Table table = openCompanyTable();
		
		Entry entry = table.newEntry();
		entry.set("company_name", company_name);
		entry.set("company_address", company_address);
		entry.set("asset_value", asset_value);
		entry.set("account", account);
		entry.set("private_key", private_key);
		entry.set("public_key", public_key);
		
		int count = table.insert(company_name, entry);
		return count;
	}
	
	function canLogin(string company_name, string private_key) public returns(int256) {
		int256 ret_code = 0;
		
		Table table = openCompanyTable();
		Entries entries = table.select(company_name, table.newCondition());
		if (1 == uint256(entries.size())) {
			if (keccak256(entries.get(0).getString("private_key")) != keccak256(private_key)) {
				ret_code = 1;
			}
		}
		else{
			ret_code = 2;
		}
        return ret_code;
    }
	
	// asset_value, company_address, private_key
	function login(string company_name) public returns(int256, string, string) {
		Table table = openCompanyTable();
		Entries entries = table.select(company_name, table.newCondition());
		return (entries.get(0).getInt("asset_value"), entries.get(0).getString("company_address"), entries.get(0).getString("private_key"));
	}
	
	function checkAsset(string company_name, int256 amount) public returns(int256){
		Table table = openCompanyTable();
		Entries entries = table.select(company_name, table.newCondition());
		if (entries.get(0).getInt("asset_value") < amount) {
			return -1;
		}
		else {
			return entries.get(0).getInt("asset_value");
		}
	}
	
	function updateAsset(string company_name, int256 asset_value) public {
		Table table = openCompanyTable();
		Entry entry = table.newEntry();
		entry.set("company_name", company_name);
		entry.set("asset_value", asset_value);
		table.update(company_name, entry, table.newCondition());
	}
	
	function hasBil(string creditor, string debtor, int256 amount)  public returns(int256) {
		Table table = openBilTable();
		Condition condition = table.newCondition();
		condition.EQ("creditor", creditor);
		condition.EQ("debtor", debtor);
		
		Entries entries = table.select(creditor, condition);
		if (0 == uint256(entries.size())) {
			return -1;
		}
		else {
			if (entries.get(0).getInt("amount") + amount < 0) {
				return -2;
			}
			return entries.get(0).getInt("amount");
		}
	}
	
	function transferBil(string new_creditor, string creditor, string debtor, int256 amount) public returns(int256) {
		if (hasCompany(new_creditor) == 0 || hasCompany(debtor) == 0 || hasCompany(creditor) == 0) {
			return -3;
		}
		
		int256 origin = hasBil(creditor, debtor, -amount);
		if (origin < 0) {
			return origin;
		}

		Table table = openBilTable();
		Entry entry = table.newEntry();
		entry.set("creditor", creditor);
		entry.set("debtor", debtor);
		entry.set("amount", origin - amount);
		
		Condition condition = table.newCondition();
		condition.EQ("creditor", creditor);
		condition.EQ("debtor", debtor);
		
		if (origin - amount == 0) {
			table.remove(creditor, condition);
		}
		else {
			table.update(creditor, entry, condition);
		}
		
		addBil(new_creditor, debtor, amount);
		
		return 0;
	}
	
	function addBil(string creditor, string debtor, int256 amount) returns(int256) {
		if (hasCompany(debtor) == 0 || hasCompany(creditor) == 0) {
			return -3;
		}

		int256 origin = hasBil(creditor, debtor, amount);
		Table table = openBilTable();
		Entry entry = table.newEntry();
		entry.set("creditor", creditor);
		entry.set("debtor", debtor);
		
		Condition condition = table.newCondition();
		condition.EQ("creditor", creditor);
		condition.EQ("debtor", debtor);
		
		if (origin < 0) {
			entry.set("amount", amount);
			table.insert(creditor, entry);
		}
		else {
			entry.set("amount", amount + origin);
			table.update(creditor, entry, condition);
		}
		return 0;
	}
	
	function repayBil(string creditor, string debtor, int256 amount) public returns(int256) {
		if (hasCompany(debtor) == 0 || hasCompany(creditor) == 0) {
			return -3;
		}
	
		if (checkAsset(debtor, amount) == -1) {
			return 1;
		}
		int256 origin = hasBil(creditor, debtor, -amount);
		if (origin < 0) {
			return origin;
		}

		Table table = openBilTable();
		Entry entry = table.newEntry();
		entry.set("creditor", creditor);
		entry.set("debtor", debtor);
		entry.set("amount", origin - amount);
		
		Condition condition = table.newCondition();
		condition.EQ("creditor", creditor);
		condition.EQ("debtor", debtor);
		
		if (origin - amount == 0) {
			table.remove(creditor, condition);
		}
		else {
			table.update(creditor, entry, condition);
		}
		updateAsset(debtor, checkAsset(debtor, amount) - amount);
		updateAsset(creditor, checkAsset(creditor, amount) + amount);

		return 0;
	}
	
	function finance(string company_name, string bank_name, int amount) public returns(int256) {
		if (hasCompany(bank_name) == 0) {
			return -3;
		}
		
		Table table = openBilTable();
		int sum_amount = 0;
		Entries entries = table.select(company_name, table.newCondition());
		for (int i = 0; i < int256(entries.size()); i++) {
			sum_amount = entries.get(i).getInt("amount") + sum_amount;
		}
		
		if (amount > sum_amount) {
			return sum_amount + 1;
		}
		
		int256 asset_value = checkAsset(bank_name, amount);
		if (asset_value == -1) {
			return -1;
		}
		updateAsset(bank_name, asset_value - amount);
		
		asset_value = checkAsset(company_name, 0);
		updateAsset(company_name, asset_value + amount);
		
		addBil(bank_name, company_name, amount);
		return 0;
	}
	
}

