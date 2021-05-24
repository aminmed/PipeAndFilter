package metier;
import java.util.*;
import pipeAndFilter.*;
import org.json.*;
public class TransactionProcessor extends Filter {
	private Data data;

    public TransactionProcessor(Data data,Pipe _dataINPipe, Pipe _dataOUTPipe ) {
    	super(); 
    	this._dataINPipe = _dataINPipe; 
    	this._dataOUTPipe = _dataOUTPipe; 
    	this.data = data; 
    }
	public boolean CheckOut (long item, long client, Date dueDate) {

		if (IsCheckedOut(item)) return false;
		//checking client sold 
		long sold = Solde(client); 
		long rentalPrice = FindItemByID(item).getRentalPrice(); 
		if (rentalPrice > sold) return false; 
		UpdateClientSold(client,sold - rentalPrice); 
		//creating new rented item in rentedItems list 
		RentedItem newRentedItem = new RentedItem(); 
		newRentedItem.setCustomerID(client);
		newRentedItem.setItemID(item);
		newRentedItem.setDueDate(dueDate);
		return true; 
	}
	public boolean UpdateClientSold (long clientID, long amount) {
		Client client = this.data.getClients().get(clientID); 
		client.setAccountBalance(client.getAccountBalance() - amount);
		return true; 
	}
	public boolean CheckIn(long itemID) {
		LinkedList<RentedItem> rentedItems = this.data.getRentedItems(); 
		RentedItem item = null; 
		for(int i=0; i< rentedItems.size(); i++) {
			item = rentedItems.get(i); 
			if (item.getItemID() == itemID) rentedItems.remove(item);  
		}
		return true; 
	}
	public boolean AddCustomer(String name, long initialSold, long id) {
	
		Client client = new Client(); 
		client.setName(name);
		client.setCustomerID(id);
		client.setAccountBalance(initialSold);
		this.data.getClients().putIfAbsent(id, client); 
		return true; 
	}
	public boolean AddStockItem(String type, String title, long rentalPrice, long itemID, String additional){
		if (type == "film") {
			Film f = new Film(); 
			f.setItemID(itemID);
			f.setRentalPrice(rentalPrice);
			f.setTitle(title);
			f.setActeur(additional);
			this.data.getStock().put(itemID,f); 
		}else {
			Jeux j = new Jeux(); 
			j.setItemID(itemID);
			j.setRentalPrice(rentalPrice);
			j.setTitle(title);
			j.setPlateforme(additional);
			this.data.getStock().put(itemID,j); 
		}
		return true; 
	}
	public StockItem FindByTitle(String title) {
		
		HashMap<Long,StockItem> stock = this.data.getStock(); // stock refers to stock hashmap 
		for(Map.Entry<Long, StockItem> entry : stock.entrySet()) {
			if(title.equals(entry.getValue().getTitle()) == true) return entry.getValue(); 
		}
		return null; 
	}
	public StockItem FindItemByID(long id) {
		
		HashMap<Long,StockItem> stock = this.data.getStock(); // stock refers to stock hashmap 
		for(Map.Entry<Long, StockItem> entry : stock.entrySet()) {
			if(id == entry.getValue().getItemID()) return entry.getValue(); 
		}
		return null; 
	}
	public Set<Film> NdByActor (String actor) {
		HashMap<Long,StockItem> stock = this.data.getStock(); // stock refers to stock hashmap
		HashSet<Film> output = new HashSet<Film>(); 
		StockItem item = null; 
		for(Map.Entry<Long, StockItem> entry : stock.entrySet()) {
			item = entry.getValue(); 
			if (item instanceof Film) {
				if ( ((Film) item).getActeur() == actor) output.add((Film) item);    
			}
		}
		return output; 
	}
	public boolean IsCheckedOut(long itemId) {
		LinkedList<RentedItem> rentedItems = this.data.getRentedItems(); 
		RentedItem item = null; 
		for(int i=0; i< rentedItems.size(); i++) {
			item = rentedItems.get(i); 
			if (item.getItemID() == itemId) return true; 
		}
		return false; 
	}
	public long Solde (long customerId) {
		HashMap<Long,Client> clients = this._getData().getClients(); 
		Client clnt = clients.get(customerId);
		return clnt.getAccountBalance(); 
	}
	public Set<RentedItem> OverdueItems (Date currentDate) {
		LinkedList<RentedItem> rentedItems = this.data.getRentedItems();
		HashSet<RentedItem> overdueItems = new HashSet<RentedItem>(); 
		RentedItem item; 
		for(int i=0; i< rentedItems.size(); i++) {
			item = rentedItems.get(i); 
			if(item.getDueDate().before(currentDate)) overdueItems.add(item); 
		}
		return overdueItems; 
	}
	
	public Data _getData() {
		return data;
	}
	public void _setData(Data data) {
		this.data = data;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		execute(); 
	}
	@Override
	public void execute() {
		// TODO Auto-generated method stub
		String input = "";
		JSONObject transaction = null;
		JSONObject answer = new JSONObject() ;
		boolean result  = false; 
		while(true) {
		    try {
		    	Thread.sleep(500);  
		    	input = this._dataINPipe.dataOUT(); 
		    	System.out.println("from transaction !"+ input); 
				try {
					transaction = new JSONObject(input);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					switch((String)transaction.get("method")) {
					case "AddCustomer":
						 result = AddCustomer((String)transaction.get("name"),
								Long.valueOf(transaction.get("initialSold").toString()),Long.valueOf(transaction.get("id").toString())); 
						break; 
					case "AddStockItem":
						result = AddStockItem((String)transaction.get("type"),(String)transaction.get("title"),
								Long.valueOf(transaction.get("rentalPrice").toString()), Long.valueOf(transaction.get("itemID").toString()),
								(String)transaction.get("additional")); 
						break;
					case "CheckOut":
						result = CheckOut((long)transaction.get("itemID"),(long)transaction.get("clientID"),
								(Date)transaction.get("dueDate")); 
						break;	
					case "CheckIn": 
						result = CheckIn((long)transaction.get("itemID")); 
						break;
					case "UpdateClientSold":
						result = UpdateClientSold(Long.valueOf(transaction.get("clientID").toString()),Long.valueOf(transaction.get("amount").toString()));
						break;
					default:
						break; 
					}
					answer.put("response", result); 
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("from transaction !"+answer.toString() ); 
				this._dataOUTPipe.dataIN(answer.toString());	
		    	
		    }catch(Exception e) {System.out.print(e);} 
		}
		
	}
}
