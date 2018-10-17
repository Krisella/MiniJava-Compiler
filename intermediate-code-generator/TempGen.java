
public class TempGen {
	
	private int cur_temp;
	
	public TempGen(int seed){
		cur_temp = seed;
	}
	
	public int getNextTemp(){
		cur_temp ++;
		return cur_temp;
	}
}
