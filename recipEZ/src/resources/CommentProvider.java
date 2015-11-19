package resources;

import java.net.URL;
import objects.Comment;
import java.util.ArrayList;
import java.io.InputStream;
import java.net.HttpURLConnection;
import org.xmlpull.v1.XmlPullParser;
import java.util.concurrent.Semaphore;
import org.xmlpull.v1.XmlPullParserFactory;

public class CommentProvider {
	private ArrayList<Comment> comments = new ArrayList<Comment>();
	private final Semaphore commentsAvailable = new Semaphore(1, true);

	public CommentProvider() {
	}

	/**
	 * Fetches all ingredients of userID
	 * @param userID
	 * @return
	 */
	public ArrayList<Comment> FetchCommentsByRecipe(String recipeID) {
		try {
			GetCommentsByRecipeID(recipeID);
			commentsAvailable.acquire();
			return comments;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}finally{
			commentsAvailable.release();
		}
	}

	/**
	 * Adds ingredientId and userId to User_Ingredients table of DB
	 * @param userId
	 * @param ingredientId
	 * @throws InterruptedException 
	 */
	public void AddIngredientToUser(final String username, final String ingredientId) {
		try {
			ExecuteGet("http://recipezrestservice-recipez.rhcloud.com/rest/IngredientServices/AddIngredientToUser/" + username + "/" + ingredientId);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void GetCommentsByRecipeID(final String recipeID) throws InterruptedException{
		ExecuteGet("http://recipezrestservice-recipez.rhcloud.com/rest/RecipeServices/GetComments/" + recipeID);
	}
	
	private void ParseCommentsFromXML(XmlPullParser myParser)  {

		int event;
		String commentName = null, recipeId = null, commentBody = null, date = null;
		Comment comment = new Comment();
		try {
			comments = new ArrayList<Comment>();
			event = myParser.getEventType();
			while (event != XmlPullParser.END_DOCUMENT) {
				String name=myParser.getName();

				switch (event){
				case XmlPullParser.START_TAG:
					if(name.equals("username")) {
						if(myParser.next() == XmlPullParser.TEXT)	commentName = myParser.getText();
					}
					else if(name.equals("recipeId")) {
						if(myParser.next() == XmlPullParser.TEXT) recipeId = myParser.getText();
					}
					else if(name.equals("commentBody")) {
						if(myParser.next() == XmlPullParser.TEXT) commentBody = myParser.getText();
					}
					else if(name.equals("date")) {
						if(myParser.next() == XmlPullParser.TEXT) date = myParser.getText();
					}
					break;

				case XmlPullParser.END_TAG:

					if(name.equals("comment")){
						comment.SetUsername(commentName);
						comment.SetRecipeID(recipeId);
						comment.SetCommentBody(commentBody);
						comment.SetDate(date);
						comments.add(comment);
						comment = new Comment();
					}
				}
				event = myParser.next();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			commentsAvailable.release();
		}
	}
	
	private void ExecuteGet(final String requestUrl) throws InterruptedException
	{
		commentsAvailable.acquire();
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					XmlPullParserFactory xmlFactoryObject;
					URL url = new URL(requestUrl);
					HttpURLConnection conn = (HttpURLConnection)url.openConnection();

					conn.setReadTimeout(10000 /* milliseconds */);
					conn.setConnectTimeout(15000 /* milliseconds */);
					conn.setRequestMethod("GET");
					conn.setDoInput(true);
					conn.connect();

					InputStream stream = conn.getInputStream();

					xmlFactoryObject = XmlPullParserFactory.newInstance();
					XmlPullParser myParser = xmlFactoryObject.newPullParser();

					myParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
					myParser.setInput(stream, null);
					ParseCommentsFromXML(myParser);
					stream.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}
}