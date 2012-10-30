/*
    Unit Library
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

    GameOfLife Unit Library: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife Unit Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife Unit Library.  If not, see <http://www.gnu.org/licenses/>.
*/

UScoreEditorGui_TransportBar {
    var <scoreView;
    var <>views, <>scoreController, <scoreViewController;

    *new{ |parent, bounds, scoreView|
        ^super.newCopyArgs(scoreView).init(parent, bounds)
    }

    init{ |parent, bounds|
        this.makeGui(parent, bounds);
        scoreViewController = SimpleController( scoreView );
        scoreViewController.put(\activeScoreChanged, {
		    this.addControllers;
		});
        this.addControllers;
    }

    score{
        ^scoreView.currentScore
    }

    addControllers{
        if(scoreController.notNil) {
            scoreController.remove;
        };
        scoreController = SimpleController( this.score );

		scoreController.put(\playState,{ |a,b,newState,oldState|
		    //[newState,oldState].postln;
		    if( newState == \playing )  {
		        views[\play].value = 1;
		        views[\pause].value = 0;
		        { views[\prepare].stop }.defer
		    };
		    if(newState == \stopped ) {
		        { views[\prepare].stop; }.defer;
                views[\pause].value = 0;
                views[\play].value = 0;
		    };
		    if( newState == \preparing  ) {
		        {�views[\prepare].start }.defer;
                views[\pause].value = 2;
		    };
		    //resuming
		    if( (newState == \playing) && (oldState == \paused) ) {
		        views[\pause].value = 0;
		    };
		    if( newState == \prepared ) {

                {�views[\prepare].stop }.defer;
                views[\play].value = 2;
		    };
		    if( newState == \paused ) {
                views[\pause].value = 1;
		    };

		});

		scoreController.put(\updatePos, { |who,what,updatePos|
		    views[\update].value = updatePos.binaryValue;
		});

		scoreController.put(\pos, { |who,what|
            views[\counter].value = this.score.pos;
		});
		
		scoreController.put(\loop, { |who,what|
            views[\loop].value = this.score.loop.binaryValue; 
		});

		views[\play].value = this.score.isPlaying.binaryValue;
		views[\pause].value = this.score.isPaused.binaryValue;
		if(this.score.isPreparing) {
		    { views[\prepare].start }.defer
		} {
		    { views[\prepare].stop }.defer
		}


    }

    remove {
        [scoreController,scoreViewController].do(_.remove)
    }

    makeGui{ |parent, bounds|

        var font = Font( Font.defaultSansFace, 11 ), view, size, marginH, marginV, playAlt;
		views = ();

		marginH = 2;
	    marginV = 2;
		size = bounds.height - (2*marginV);
        view = CompositeView( parent, bounds );
        
        RoundView.pushSkin( UChainGUI.skin );

		view.addFlowLayout(marginH@marginV);
		//view.background_( Color.white );
		view.resize_(8);



        views[\prepare] = WaitView( view, size@size )
					.alphaWhenStopped_( 0 )
					.canFocus_(false);

		views[\play] = SmoothSimpleButton( view, 40@size  )
			.states_( [
			    [ \play, Color.black, Color.clear ],
			    [ \stop, Color.black, Color.green.alpha_(0.5) ],
			    [ \play, Color.blue, Color.red.alpha_(0.5) ]] )
			.canFocus_(false)
			.font_( font )
			//.changeStateWhenPressed_(false)
			.action_({  |v,c,d,e|

			    var startedPlaying;
			    switch( v.value )
			        {0} {
                        this.score.prepareAndStart( ULib.servers, this.score.pos, true);
			        }{1} {
                        this.score.stop;
                    }{2} {
                        this.score.start( ULib.servers, this.score.pos, true);
                    }

			});
			
		views[\pause] = SmoothSimpleButton( view, 50@size  )
			.states_( [
			    [ \pause, Color.black, Color.clear ],
			    [ \pause, Color.red,Color.green.alpha_(0.5) ],
			    [ \pause, Color.blue,Color.red.alpha_(0.5) ]] )
			.canFocus_(false)
			.font_( font )
			.action_({ |v|
			    switch( v.value)
			    {0}{
			        if(this.score.isPlaying) {
			            this.score.pause;
			       } {
			            this.score.prepare;
			       }
			    }{1} {
			        this.score.resume(ULib.servers);
			    }{2}{
			        this.score.stop;
			    }
			});

		views[\return] = SmoothButton( view, 50@size  )
			.states_( [[\return, Color.black, Color.clear ]])
			.canFocus_(false)
			.font_( font )
			.action_({
			    this.score.pos = 0;
			});

        views[\loop] = SmoothButton( view, 50@size  )
        			.states_( [[\roundArrow, Color.black, Color.clear ],
        			[\roundArrow, Color.black, Color.green.alpha_(0.5) ]])
        			.value_( this.score.loop.binaryValue )
        			.canFocus_(false)
        			.font_( font )
        			.action_({ |v| this.score.loop = v.value.booleanValue;  });

        view.decorator.shift(20,0);

	    views[\counter] = SMPTEBox( view, 150@size )
			.value_( this.score.pos )
			.radius_( 12 )
			.align_( \center )
			.clipLo_(0)
			.background_( Color.clear )
			.charSelectColor_( Color.white.alpha_(0.5) )
			.autoScale_( true )
            .action_({ |v|
                if(this.score.isStopped) {
                    this.score.pos = v.value
                }
            });
        StaticText(view, 60@size )
            .string_("Update:")
            .align_('right');
        views[\update] = SmoothButton( view, size@size )
            .label_( [ "", 'x' ] )
            .value_(1)
            .radius_( bounds.height / 8 )
            .action_({ |bt| this.score.updatePos = bt.value.booleanValue; })
            .canFocus_(false);
            
        // this should become MVC
        StaticText(view, 60@size )
            .string_("OSC:")
            .align_('right');
        views[\osc] = SmoothButton( view, size@size )
            .label_( [ "", 'x' ] )
            .value_(this.score.oscSetter.notNil.binaryValue )
            .radius_( bounds.height / 8 )
            .action_({ |bt| switch( bt.value.asInt,
	            	1, { this.score.enableOSC },
	            	0, {  this.score.disableOSC }
	            )
            })
            .canFocus_(false);
        
        RoundView.popSkin;

    }

}