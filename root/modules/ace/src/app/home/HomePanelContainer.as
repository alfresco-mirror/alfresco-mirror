package app.home
{
	import mx.containers.Canvas;
	import mx.containers.VBox;
	import flash.display.DisplayObject;
	import mx.controls.Alert;
	import mx.core.IChildList;
	import mx.utils.DisplayUtil;
	import util.error.ErrorService;
	import component.util.SimpleGradientBorder;
	import component.util.GlossGradientBorder;
	import mx.core.EdgeMetrics;
	import mx.states.State;
	import mx.states.SetProperty;
	import flash.events.Event;
	import mx.states.Transition;
	import mx.effects.Parallel;
	import mx.effects.Move;
	import mx.effects.WipeUp;
	import mx.effects.Resize;
	import mx.controls.LinkBar;
	import mx.effects.Fade;
	import mx.events.FlexEvent;
	import util.authentication.LoginCompleteEvent;
	import util.authentication.AuthenticationService;
	import mx.effects.Pause;
	import mx.effects.Sequence;
	import mx.events.EffectEvent;
	import mx.effects.Effect;
	import flash.events.MouseEvent;
	import flash.events.FocusEvent;
	import mx.core.Container;
	import mx.events.StateChangeEvent;
	import mx.effects.effectClasses.FadeInstance;

	public class HomePanelContainer extends Canvas
	{
		public static const STATE_START:String = "startState";
		public static const STATE_COLLAPSED:String = "collapsedState";
		
		private static const TRANSITION_SPEED:Number = 495;
		
		/** UI controls */
		private var _vBox:VBox;
		private var _topCanvas:HomePanelTop;
		private var _mainCanvas:Canvas;
		private var _bottomCanvas:HomePanelBottom;
		
		/** Transitions */
		private var _collapseResize:Resize;
		private var _showResize:Sequence;
		
		private var _transformationRunning:Boolean = false;
		
		private var _showPause:Number = 1;	
		
		private var _hasFocus:Boolean = true;	
		
		private var _innerHeight:Number = 100;
		
		private var _showing:Boolean = false;
		
		/**
		 * Constructor
		 */
		public function HomePanelContainer()
		{
			super();		
		}
		
		public function set showPause(value:Number):void
		{
			this._showPause = value;
		}
		
		public function get showPause():Number
		{
			return this._showPause;	
		}
		
		public function set hasFocus(value:Boolean):void
		{
			if (value != this._hasFocus)
			{
				this._hasFocus = value;
				updateFocus();
			}
		}
		
		public function get hasFocus():Boolean
		{
			return this._hasFocus;
		}
		
		public function set innerHeight(value:Number):void
		{
			this._innerHeight = value;
		}
		
		public function get innerHeight():Number
		{
			return this._innerHeight;	
		}
		
		/**
		 * Create children override
		 */
		override protected function createChildren():void
		{			
			try
			{				
				// Create the main VBox used for layout
				this._vBox = new VBox();
				this._vBox.percentHeight = 100;
				this._vBox.percentWidth = 100;					
				this._vBox.setStyle("verticalGap", 0);
				this._vBox.setStyle("horizontalGap", 0);	
				this._vBox.setStyle("borderStyle", "none");
				this._vBox.setStyle("borderWidth", 0);
				
				// Create the top bar 
				this._topCanvas = new HomePanelTop();
				this._topCanvas.percentWidth = 100;
				this._topCanvas.height = 23;	
				this._topCanvas.title= this.label;	
				
				// Create the main canvas where the content of the panel resides
				this._mainCanvas = new Canvas();
				this._mainCanvas.percentWidth = 100;
				this._mainCanvas.height = this._innerHeight;
				this._mainCanvas.setStyle("borderStyle", "none");
				this._mainCanvas.setStyle("borderWidth", 0);
	
				// Create the botton bar
				this._bottomCanvas = new HomePanelBottom();
				this._bottomCanvas.percentWidth = 100;
				this._bottomCanvas.height = 23;			
				
				// Place the controls in the VBox
				this._vBox.addChild(this._topCanvas);
				this._vBox.addChild(this._mainCanvas);
				this._vBox.addChild(this._bottomCanvas);					
				
				// Call the super method
				super.createChildren();
				
				// Add the VBox 
				this.addChild(this._vBox);
				
				// Copy any controls found on the origional canvas into the main canvas area
				var i:int;
				for (i = 0; i < numChildren; i++) 
				{
		    		var control:DisplayObject = getChildAt(i);					
		    		this.removeChild(control);
					this._mainCanvas.addChild(control);					
				}
								
				// Initialise the states and transitions
				initStatesAndTransitions();
				
				// Register interest in events
				this._topCanvas.addEventListener(HomePanelTopClass.MINMAX_CLICK_EVENT, onMinMaxClick);
				AuthenticationService.instance.addEventListener(LoginCompleteEvent.LOGIN_COMPLETE, onLoginComplete);
				this._bottomCanvas.addEventListener(EffectEvent.EFFECT_START, onEffectStart);
				this._bottomCanvas.addEventListener(EffectEvent.EFFECT_END, onEffectEnd);
				this._vBox.addEventListener(EffectEvent.EFFECT_END, onEffectStart);
				this._vBox.addEventListener(EffectEvent.EFFECT_END, onEffectEnd);
				this.addEventListener(StateChangeEvent.CURRENT_STATE_CHANGE, onCurrentStateChange);
				
				// Set the intiial state
				currentState = STATE_START;
			}
			catch (error:Error)
			{
				// Raise any errors with the error service
				ErrorService.instance.raiseError(ErrorService.APPLICATION_ERROR, error);
			}
		}
		
		private function initStatesAndTransitions():void
		{
			// Create collapsed state
			var collapsed:State = new State();
			collapsed.name = STATE_COLLAPSED;
			collapsed.overrides.push(new SetProperty(this._mainCanvas, "height", 0));	
			collapsed.overrides.push(new SetProperty(this._bottomCanvas, "alpha", 0.2));
			
			// Create start state
			var start:State = new State();
			start.name = STATE_START;
			start.overrides.push(new SetProperty(this._mainCanvas, "height", 0));	
			start.overrides.push(new SetProperty(this._vBox, "alpha", 0.0));
			
			// Add the states
			this.states.push(collapsed);
			this.states.push(start);
			
			// Create the pause effect
			var pause:Pause = new Pause();
			pause.duration = this._showPause;
			pause.targets = new Array(this._vBox, this, this._bottomCanvas, this._topCanvas);
			
			// Create the collapse resize effect
			this._collapseResize = new Resize();
			this._collapseResize.duration = TRANSITION_SPEED;
			this._collapseResize.target = this._mainCanvas;	
			this._collapseResize.suspendBackgroundProcessing = true;	
			
			// Create the show resize effect
			this._showResize = new Sequence();
			this._showResize.addChild(pause);
			this._showResize.addChild(this._collapseResize);
			
			// Create fade effect
			var fade:Fade = new Fade();
			fade.duration = TRANSITION_SPEED;
			fade.target = this._bottomCanvas;
			
			var fade2:Fade = new Fade();
			fade2.duration = TRANSITION_SPEED;
			fade2.target = this._vBox;
			
			// Create toCollapse transition
			var toCollapse:Transition = new Transition();
			toCollapse.fromState = "";
			toCollapse.toState = STATE_COLLAPSED;	
			toCollapse.effect = fade;
			
			// Create fromCollapse transition										
			var fromCollapse:Transition = new Transition();
			fromCollapse.fromState = STATE_COLLAPSED;
			fromCollapse.toState = "";	
			fromCollapse.effect = fade;					
			
			// From start transition
			var fromStartSeq:Sequence = new Sequence();
			fromStartSeq.addChild(pause);
			fromStartSeq.addChild(fade2);				
			var fromStart:Transition = new Transition();
			fromStart.fromState = STATE_START;
			fromStart.toState = "*";	
			fromStart.effect = fromStartSeq;		
			
			// Add to transition list
			this.transitions.push(toCollapse);
			this.transitions.push(fromCollapse);	
			this.transitions.push(fromStart);
		}
		
		private function onEffectStart(event:EffectEvent):void
		{
			// Set the transaction running marker flag
			this._transformationRunning = true;
		}
		
		private function onEffectEnd(event:EffectEvent):void
		{
			// Remove the resize effect
			this._mainCanvas.setStyle("resizeEffect", null);
			
			// Clear the transaction running flag	
			this._transformationRunning = false;
			
			if (this._showing == true && (event.effectInstance is FadeInstance))
			{
				this._showing = false;
				showComplete();
			}
		}			
		
		private function onCurrentStateChange(event:StateChangeEvent):void
		{	
			if (event.oldState == STATE_START)
			{
				this._showing = true;
			}
		}
		
		protected function showComplete():void
		{
			// Empty implementation	
		}
		
		/**
		 * On minMax click button event handler
		 */
		private function onMinMaxClick(event:Event):void
		{
			if (this._transformationRunning == false)
			{
				// Need to add this transition separatly to avoid flicker		
				this._mainCanvas.setStyle("resizeEffect", this._collapseResize);
				
				// Switch the state appropriatly
				if (this.currentState == STATE_COLLAPSED)
				{
					this.currentState = null;
				}
				else
				{
					this.currentState = STATE_COLLAPSED;	
				}
			}
		}	
		
		private function onLoginComplete(event:Event):void	
		{
			this._mainCanvas.setStyle("resizeEffect", this._showResize);
				
			if (currentState == STATE_START)
			{
				this.currentState = null;
			}
		}
		
		private function updateFocus():void
		{
			if (this.currentState == "" || this.currentState == STATE_COLLAPSED && this._vBox != null)
			{
				if (this._hasFocus == true)
				{
					this._vBox.alpha = 1.0;
				}
				else
				{
					this._vBox.alpha = 0.4;
				}
			}
		}
	}
}