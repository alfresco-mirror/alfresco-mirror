/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.encryption;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This wraps a Java Keystore and caches the encryption keys. It manages the loading and caching of the encryption keys.
 * 
 * @since 4.0
 *
 */
public class AlfrescoKeyStoreImpl implements AlfrescoKeyStore
{
    private static final Log logger = LogFactory.getLog(AlfrescoKeyStoreImpl.class);
    
    protected KeyStoreParameters keyStoreParameters;    
    protected KeyResourceLoader keyResourceLoader;

    protected Map<String, Key> keys;
    protected final WriteLock writeLock;
    protected final ReadLock readLock;

    public AlfrescoKeyStoreImpl()
    {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        writeLock = lock.writeLock();
        readLock = lock.readLock();
        this.keys = new HashMap<String, Key>(7);
    }

    public AlfrescoKeyStoreImpl(KeyStoreParameters keyStoreParameters, KeyResourceLoader keyResourceLoader)
    {
    	this();

        this.keyResourceLoader = keyResourceLoader;
        this.keyStoreParameters = keyStoreParameters;

        safeInit();
    }

    public void setKeyStoreParameters(KeyStoreParameters keyStoreParameters)
	{
		this.keyStoreParameters = keyStoreParameters;
	}

	public void setKeyResourceLoader(KeyResourceLoader keyResourceLoader)
	{
		this.keyResourceLoader = keyResourceLoader;
	}

	public int reload()
    {
    	writeLock.lock();
    	try
    	{
    		// make sure key cache is cleared
    		keys.clear();

    		// reload keys
    		return loadKeyStore();
    	}
    	finally
    	{
    		writeLock.unlock();
    	}
    }

	public KeyStoreParameters getkeyStoreParameters()
	{
		return keyStoreParameters;
	}

	public KeyResourceLoader getKeyResourceLoader()
	{
		return keyResourceLoader;
	}
	
    public String getName()
    {
    	return keyStoreParameters.getName();
    }

	public String getLocation()
	{
		return keyStoreParameters.getLocation();
	}
	
	public String getProvider()
	{
		return keyStoreParameters.getProvider();
	}
	
	public String getType()
	{
		return keyStoreParameters.getType();
	}
	
	public String getKeyMetaDataFileLocation()
	{
		return keyStoreParameters.getKeyMetaDataFileLocation();
	}

	protected InputStream getKeyStoreStream() throws FileNotFoundException
	{
		if(getLocation() == null)
		{
			return null;
		}
		return keyResourceLoader.getKeyStore(getLocation());
	}

    public Set<String> getKeyAliases()
    {
    	return new HashSet<String>(keys.keySet());
    }
    
    protected KeyInfoManager getKeyInfoManager() throws FileNotFoundException, IOException
    {
    	return new KeyInfoManager();
    }

    protected int cacheKeys(KeyStore ks, KeyInfoManager keyInfoManager) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException
    {
    	int numKeysCached = 0;

        writeLock.lock();
        try
        {
        	// load and cache the keys
        	for(Entry<String, KeyInformation> keyEntry : keyInfoManager.getKeyInfo().entrySet())
        	{
            	String keyAlias = keyEntry.getKey();

            	KeyInformation keyInfo = keyInfoManager.getKeyInformation(keyAlias);
                String passwordStr = keyInfo != null ? keyInfo.getPassword() : null;

                // Null is an acceptable value (means no key)
                Key key = null;

                // Attempt to get the key
                key = ks.getKey(keyAlias, passwordStr == null ? null : passwordStr.toCharArray());
                keys.put(keyAlias, key);
                // Key loaded
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Retrieved key from keystore: \n" +
                            "   Location: " + getLocation() + "\n" +
                            "   Provider: " + getProvider() + "\n" +
                            "   Type:     " + getType() + "\n" +
                            "   Alias:    " + keyAlias + "\n" +
                            "   Password?: " + (passwordStr != null));

                    Certificate[] certs = ks.getCertificateChain(keyAlias);
                    if(certs != null)
                    {
                    	logger.debug("Certificate chain '" + keyAlias + "':");
                    	for(int c = 0; c < certs.length; c++)
                    	{
                    		if(certs[c] instanceof X509Certificate)
                    		{
                    			X509Certificate cert = (X509Certificate)certs[c];
                    			logger.debug(" Certificate " + (c + 1) + ":");
                    			logger.debug("  Subject DN: " + cert.getSubjectDN());
                    			logger.debug("  Signature Algorithm: " + cert.getSigAlgName());
                    			logger.debug("  Valid from: " + cert.getNotBefore() );
                    			logger.debug("  Valid until: " + cert.getNotAfter());
                    			logger.debug("  Issuer: " + cert.getIssuerDN());
                    		}
                    	}
                    }
                }
                
                numKeysCached++;
            }

            return numKeysCached;
        }
        finally
        {
            writeLock.unlock();
        }
    }

    protected KeyStore initialiseKeyStore()
    {
    	KeyStore ks = null;

    	try
    	{
	        if(getProvider() == null)
	        {
	            ks = KeyStore.getInstance(getType());
	        }
	        else
	        {
	            ks = KeyStore.getInstance(getType(), getProvider());
	        }

	        ks.load(null, null);

	        return ks;
    	}
		catch(Throwable e)
		{
			throw new AlfrescoRuntimeException("Unable to intialise key store", e);
		}
    }

    protected KeyStore loadKeyStore(KeyInfoManager keyInfoManager)
    {
    	String pwdKeyStore = null;

    	try
    	{
	    	KeyStore ks = initialiseKeyStore();

	        // Load it up
	        InputStream is = getKeyStoreStream();
	        if(is != null)
	        {
		        // Get the keystore password
		        pwdKeyStore = keyInfoManager.getKeyStorePassword();
		        ks.load(is, pwdKeyStore == null ? null : pwdKeyStore.toCharArray());
	        }
	        else
	        {
	        	// this is ok, the keystore will contain no keys.
	        	logger.warn("Keystore file doesn't exist: " + getLocation());
	        }

	        return ks;
    	}
		catch(Throwable e)
		{
			throw new AlfrescoRuntimeException("Unable to load key store", e);
		}
        finally
        {
        	pwdKeyStore = null;
        }
    }

    public void init()
    {
    	writeLock.lock();
    	try
    	{
    		safeInit();
    	}
    	finally
    	{
    		writeLock.unlock();
    	}
    }
    
    /**
     * Initializes class
     */
    private void safeInit()
    {
    	loadKeyStore();
    }

    private int loadKeyStore()
    {
        InputStream is = null;
        KeyInfoManager keyInfoManager = null;
        KeyStore ks = null;

        PropertyCheck.mandatory(this, "location", getLocation());

        if (!PropertyCheck.isValidPropertyString(getLocation()))
        {
            keyStoreParameters.setLocation(null);
        }
        if (!PropertyCheck.isValidPropertyString(getProvider()))
        {
        	keyStoreParameters.setProvider(null);
        }
        if (!PropertyCheck.isValidPropertyString(getType()))
        {
        	keyStoreParameters.setType(null);
        }
        if (!PropertyCheck.isValidPropertyString(getKeyMetaDataFileLocation()))
        {
        	keyStoreParameters.setKeyMetaDataFileLocation(null);
        }

        // Make sure we choose the default type, if required
        if(getType() == null)
        {
            keyStoreParameters.setType(KeyStore.getDefaultType());
        }

        try
        {
            keyInfoManager = getKeyInfoManager();
	        ks = loadKeyStore(keyInfoManager);
            // Loaded
        }
        catch (Throwable e)
        {
            throw new AlfrescoRuntimeException(
                    "Failed to initialize keystore: \n" +
                    "   Location: " + getLocation() + "\n" +
                    "   Provider: " + getProvider() + "\n" +
                    "   Type:     " + getType(),
                    e);
        }
        finally
        {
            if(keyInfoManager != null)
            {
            	keyInfoManager.clearKeyStorePassword();
            }

            if (is != null)
            {
                try
                {
                	is.close();
                }
                catch (Throwable e)
                {
                	
                }
            }
        }
		
        try
        {
        	// cache the keys from the keystore
        	int numKeys = cacheKeys(ks, keyInfoManager);

    		if(logger.isDebugEnabled())
    		{
                logger.debug(
                        "Initialized keystore: \n" +
                        "   Location: " + getLocation() + "\n" +
                        "   Provider: " + getProvider() + "\n" +
                        "   Type:     " + getType() + "\n" +
                        keys.size() + " keys found");
    		}

    		return numKeys;
        }
        catch(Throwable e)
        {
            throw new AlfrescoRuntimeException(
                    "Failed to retrieve keys from keystore: \n" +
                    "   Location: " + getLocation() + "\n" +
                    "   Provider: " + getProvider() + "\n" +
                    "   Type:     " + getType() + "\n",
                    e);
        }
        finally
        {
	        // Clear key information
	        keyInfoManager.clear();
        }
    }
    
	public void create()
	{
		KeyInfoManager keyInfoManager = null;

		try
		{
			if(getKeyStoreStream() == null)
			{
		        keyInfoManager = getKeyInfoManager();
	
		        KeyStore ks = initialiseKeyStore();
	
		        String keyStorePassword = keyInfoManager.getKeyStorePassword();
		        if(keyStorePassword == null)
		        {
		        	throw new AlfrescoRuntimeException("Key store password is null for keystore at location " + getLocation()
		        			+ ", key store meta data location" + getKeyMetaDataFileLocation());
		        }
	
				// Add keys from the passwords file to the keystore
				for(Map.Entry<String, AlfrescoKeyStoreImpl.KeyInformation> keyEntry : keyInfoManager.getKeyInfo().entrySet())
				{
					KeyInformation keyInfo = keyInfoManager.getKeyInformation(keyEntry.getKey());
			        String keyPassword = keyInfo.getPassword();
			        if(keyPassword == null)
			        {
			        	throw new AlfrescoRuntimeException("No password found for encryption key " + keyEntry.getKey());
			        }
		        	Key key = generateSecretKey(keyEntry.getValue());
		        	ks.setKeyEntry(keyInfo.getAlias(), key, keyInfo.getPassword().toCharArray(), null);
				}
		
		        ks.store(new FileOutputStream(getLocation()), keyStorePassword.toCharArray());
	
		        cacheKeys(ks, keyInfoManager);
			}
			else
			{
				logger.warn("Key store " + getLocation() + " already exists.");
			}
		}
		catch(Throwable e)
		{
            throw new AlfrescoRuntimeException(
                    "Failed to create keystore: \n" +
                    "   Location: " + getLocation() + "\n" +
                    "   Provider: " + getProvider() + "\n" +
                    "   Type:     " + getType(),
                    e);
		}
		finally
		{
			if(keyInfoManager != null)
			{
				keyInfoManager.clear();
			}
		}
	}
	
    public boolean exists()
    {
    	try
    	{
    		return (getKeyStoreStream() != null);
    	}
    	catch(FileNotFoundException e)
    	{
    		return false;
    	}
    }
	
    public Key getKey(String keyAlias)
    {
    	readLock.lock();
    	try
    	{
    		return keys.get(keyAlias);
    	}
    	finally
    	{
    		readLock.unlock();
    	}
    }

	public KeyManager[] createKeyManagers()
	{
		KeyInfoManager keyInfoManager = null;

		try
		{
			keyInfoManager = getKeyInfoManager();
			KeyStore ks = loadKeyStore(keyInfoManager);

			logger.debug("Initializing key managers");
			KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			
			String keyStorePassword = keyInfoManager.getKeyStorePassword();
			kmfactory.init(ks, keyStorePassword != null ? keyStorePassword.toCharArray(): null);
			return kmfactory.getKeyManagers(); 
		}
		catch(Throwable e)
		{
			throw new AlfrescoRuntimeException("Unable to create key manager", e);
		}
		finally
		{
			if(keyInfoManager != null)
			{
				keyInfoManager.clear();
			}
		}
	}
	
	public TrustManager[] createTrustManagers()
	{
		KeyInfoManager keyInfoManager = null;

		try
		{
			keyInfoManager = getKeyInfoManager();
			KeyStore ks = loadKeyStore(keyInfoManager);

			logger.debug("Initializing trust managers");
			TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
					TrustManagerFactory.getDefaultAlgorithm());
			tmfactory.init(ks);
			return tmfactory.getTrustManagers();
		}
		catch(Throwable e)
		{
			throw new AlfrescoRuntimeException("Unable to create key manager", e);
		}
		finally
		{
			if(keyInfoManager != null)
			{
				keyInfoManager.clear();
			}
		}
	}

	protected Key generateSecretKey(KeyInformation keyInformation) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException
    {
		DESedeKeySpec keySpec = new DESedeKeySpec(keyInformation.getKeyData());	
    	SecretKeyFactory kf = SecretKeyFactory.getInstance(keyInformation.getKeyAlgorithm());
    	SecretKey secretKey = kf.generateSecret(keySpec);
    	return secretKey;
    }
	
	public void importPrivateKey(String keyAlias, String keyPassword, InputStream fl, InputStream certstream)
	throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, KeyStoreException
	{
		KeyInfoManager keyInfoManager = null;

    	writeLock.lock();
    	try
    	{
            keyInfoManager = getKeyInfoManager();
	        KeyStore ks = loadKeyStore(keyInfoManager);

	        // loading Key
	        byte[] keyBytes = new byte[fl.available()];
	        KeyFactory kf = KeyFactory.getInstance("RSA");
	        fl.read(keyBytes, 0, fl.available());
	        fl.close();
	        PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(keyBytes);
	        PrivateKey key = kf.generatePrivate(keysp);
	
	        // loading CertificateChain
	        CertificateFactory cf = CertificateFactory.getInstance("X.509");
	
	        @SuppressWarnings("rawtypes")
			Collection c = cf.generateCertificates(certstream) ;
	        Certificate[] certs = new Certificate[c.toArray().length];
	
	        certs = (Certificate[])c.toArray(new Certificate[0]);
	
	        // storing keystore
	        ks.setKeyEntry(keyAlias, key, keyPassword.toCharArray(), certs);

	        if(logger.isDebugEnabled())
	        {
	        	logger.debug("Key and certificate stored.");
	        	logger.debug("Alias:"+ keyAlias);
	        }

	        ks.store(new FileOutputStream(getLocation()), keyPassword.toCharArray());
    	}
    	finally
    	{
            if(keyInfoManager != null)
            {
            	keyInfoManager.clear();
            }

    		writeLock.unlock();
    	}
	}

    public static class KeyInformation
    {
    	protected String alias;
    	protected byte[] keyData;
    	protected String password;
    	protected String keyAlgorithm;

		public KeyInformation(String alias, byte[] keyData, String password, String keyAlgorithm)
		{
			super();
			this.alias = alias;
			this.keyData = keyData;
			this.password = password;
			this.keyAlgorithm = keyAlgorithm;
		}

		public String getAlias()
		{
			return alias;
		}
		
		public byte[] getKeyData()
		{
			return keyData;
		}

		public String getPassword()
		{
			return password;
		}

		public String getKeyAlgorithm()
		{
			return keyAlgorithm;
		}
    }

    /*
     * Caches key meta data information such as password, seed.
     *
     */
    protected class KeyInfoManager
    {
    	private Properties keyProps;
    	private String keyStorePassword = null;
    	private Map<String, KeyInformation> keyInfo;

    	/**
    	 * For testing.
    	 * 
    	 * @param passwords
    	 * @throws IOException
    	 * @throws FileNotFoundException
    	 */
    	KeyInfoManager(Map<String, String> passwords) throws IOException, FileNotFoundException
    	{
    		keyInfo = new HashMap<String, KeyInformation>(2);
    		for(Map.Entry<String, String> password : passwords.entrySet())
    		{
    			keyInfo.put(password.getKey(), new KeyInformation(password.getKey(), null, password.getValue(), null));
    		}
    	}

    	KeyInfoManager() throws IOException, FileNotFoundException
    	{
    		keyInfo = new HashMap<String, KeyInformation>(2);
    		loadKeyMetaData();
    	}

    	public Map<String, KeyInformation> getKeyInfo()
    	{
    		// TODO defensively copy
    		return keyInfo;
    	}

    	/**
         * Set the map of key meta data (including passwords to access the keystore).
         * <p/>
         * Where required, <tt>null</tt> values must be inserted into the map to indicate the presence
         * of a key that is not protected by a password.  They entry for {@link #KEY_KEYSTORE_PASSWORD}
         * is required if the keystore is password protected.
         * 
         * @param passwords             a map of passwords including <tt>null</tt> values
         */
    	protected void loadKeyMetaData() throws IOException, FileNotFoundException
    	{
    		keyProps = keyResourceLoader.loadKeyMetaData(getKeyMetaDataFileLocation());
    		if(keyProps != null)
    		{
	    		String aliases = keyProps.getProperty("aliases");
	    		if(aliases == null)
	    		{
	    			throw new AlfrescoRuntimeException("Passwords file must contain an aliases key");
	    		}
	
	    		this.keyStorePassword = keyProps.getProperty(KEY_KEYSTORE_PASSWORD);
	    		
	    		StringTokenizer st = new StringTokenizer(aliases, ",");
	    		while(st.hasMoreTokens())
	    		{
	    			String keyAlias = st.nextToken();
	    			keyInfo.put(keyAlias, loadKeyInformation(keyAlias));
	    		}
    		}
    		else
    		{
    			//throw new FileNotFoundException("Cannot find key metadata file " + getKeyMetaDataFileLocation());
    		}
    	}
    	
    	public void clear()
    	{
    		this.keyStorePassword = null;
    		if(this.keyProps != null)
    		{
    			this.keyProps.clear();
    		}
    	}

    	public void removeKeyInformation(String keyAlias)
    	{
    		this.keyProps.remove(keyAlias);
    	}

    	protected KeyInformation loadKeyInformation(String keyAlias)
    	{
            String keyPassword = keyProps.getProperty(keyAlias + ".password");
            String keyData = keyProps.getProperty(keyAlias + ".keyData");
            String keyAlgorithm = keyProps.getProperty(keyAlias + ".algorithm");

            byte[] keyDataBytes = null;
            if(keyData != null)
            {
            	keyDataBytes = Base64.decodeBase64(keyData);
            }
            KeyInformation keyInfo = new KeyInformation(keyAlias, keyDataBytes, keyPassword, keyAlgorithm);
            return keyInfo;
    	}

    	public String getKeyStorePassword()
    	{
    		return keyStorePassword;
    	}
    	
    	public void clearKeyStorePassword()
    	{
    		this.keyStorePassword = null;
    	}

    	public KeyInformation getKeyInformation(String keyAlias)
    	{
    		return keyInfo.get(keyAlias);
    	}
    }
}
