public class ClientData implements java.io.Serializable
{
	public java.util.ArrayList<javax.crypto.spec.SecretKeySpec> encryptKs;
	
	ClientData(java.util.ArrayList<javax.crypto.spec.SecretKeySpec> e)
	{
		encryptKs = e;
	}
}
