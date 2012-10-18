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


/*

  View that shows events of a score as rectangles with rounded corners.

*/
UScoreView {

     var <scoreEditorsList;
     var <usessionMouseEventsManager;
     var <>snapActive, <>snapH;
     var <>numTracks = 12;
     var <scoreView, <scoreListView, <mainComposite, font, <parent, <bounds;
     var <>scoreList;
     var <currentScoreEditorController, <scoreController, <eventControllers = #[];

     *new{ |parent, bounds, scoreEditor| ^super.new.init(scoreEditor, parent,bounds) }

     init { |scoreEditor, inParent, inBounds|

        scoreEditorsList = [scoreEditor];
        snapActive = true;
		snapH = 0.25;
        font = Font( Font.defaultSansFace, 11 );
		scoreList = [scoreEditor.score];
		parent = inParent;
		bounds = inBounds;
		this.addCurrentScoreControllers;

     }

     addCurrentScoreControllers {

         if(this.isInnerScore){
            if( currentScoreEditorController.notNil ) {
                currentScoreEditorController.remove;
            };
            currentScoreEditorController = SimpleController( scoreEditorsList.last );
            currentScoreEditorController.put(\preparingToChangeScore, {
                    this.baseEditor.storeUndoState;
            });
        };


        if( scoreController.notNil ) {
	        scoreController.remove;
	    };
        scoreController = SimpleController( this.currentScore );

        scoreController.put(\pos, {
		    { this.update }.defer;
		});

		scoreController.put(\events, {
            this.makeEventControllers;
        });

		scoreController.put(\something, {
		    this.update;
		    if(this.isInnerScore){
		        this.baseEditor.changed(\score);
		    }
		});
	}

	makeEventControllers {
	    eventControllers.do(_.remove);
	    eventControllers = this.currentScore.collect{ |e|
            var s = SimpleController(e);
            [\startTime,\dur,\fadeIn,\fadeOut,\name].do{ |key|
                s.put(key,{ { this.update }.defer; })
            };
            s
        }
	}

	remove {
        ([currentScoreEditorController, scoreController, usessionMouseEventsManager]++eventControllers).do(_.remove)
    }

	update {
	    scoreView.refresh;
	}

    currentEditor{
        ^scoreEditorsList.last
    }

    baseEditor{
        ^scoreEditorsList[0]
    }

    currentScore{
        ^scoreEditorsList.last.score
    }

    isInnerScore{
        ^(scoreEditorsList.size > 1)
    }

    selectedEvents{ ^usessionMouseEventsManager.selectedEvents }
    selectedEventsOrAll { ^usessionMouseEventsManager.selectedEventsOrAll }

    editSelected{
        var event, events = this.selectedEvents, gui, currentScore, chains;
        switch(events.size)
            {0}{ 
	            currentScore = this.currentScore;
	            chains = currentScore.getAllUChains;
	            if( chains.size > 0 ) {
	           	 gui = MassEditUChain( chains ).gui;
	                 gui.windowName = "MassEditUChain : % (all % events)".format( 
	                    currentScore.name, 
	                    chains.size
	                 );
	            };
	        }
            {1}{
                event = events[0];
                if(event.isFolder){
                    gui = MassEditUChain(event.getAllUChains).gui( score: event );
                    currentScore = this.currentScore;
                    gui.windowName = "MassEditUChain : % [ % ]".format( 
                    	currentScore.name, 
                    	currentScore.events.indexOf( event )
                    );
                } {
                    gui = event.gui;
                    currentScore = this.currentScore;
                    gui.windowName = "UChain : % [ % ]".format( 
                    	currentScore.name, 
                    	currentScore.events.indexOf( event )
                    );
                }
            }
            { 
	            
	            gui = MassEditUChain(events.collect(_.getAllUChains).flat).gui;
                 gui.windowName = "MassEditUChain : % ( % events )".format( 
                    this.currentScore.name, 
                    gui.chain.uchains.size
                 );
	       }

    }

	deleteSelected{
	    ^this.currentEditor.deleteEvents(this.selectedEvents)
	}

	selectAll {
	    usessionMouseEventsManager.eventViews.do(_.selected = true);
	    this.update;
	}

    selectSimilar {
        var selectedTypes = this.selectedEvents.collect{ |x| x.units.collect(_.defName) };
        usessionMouseEventsManager.eventViews.do{ |evView|
            if( selectedTypes.includesEqual(evView.event.units.collect(_.defName)) ) {
                evView.selected = true
            }
        };
        this.update
    }

    disableSelected {
        this.currentEditor.disableEvents(this.selectedEvents)
    }

    enableSelected {
        this.currentEditor.enableEvents(this.selectedEvents)
    }

    soloEnableSelected {
        this.currentEditor.soloEnableEvents(this.selectedEvents)
    }

    duplicateSelected {
        this.currentEditor.duplicateEvents(this.selectedEvents)
    }

    addTrack {
        numTracks = numTracks + 1;
        this.update;
    }

    removeUnusedTracks {
        numTracks = ((this.currentScore.events.collect( _.track )
            .maxItem ? 10) + 2).max( 12 );
        this.update;
    }

    openSelectedSubScoreInNewWindow{
        this.selectedEvents !? { |x|
            var y = x.at(0);
            if(y.isFolder) {
                UScoreEditorGUI(UScoreEditor(y))
            }
        }

    }


     // call to initialize and draw view. this mean the views are only actually drawn when this method is called after creating this instance.
     // This is needed to be able to pass an instance of this class to the topbar object.
    makeView{
        mainComposite = CompositeView(parent,bounds).resize_(5);
        this.makeScoreView
    }

     remake{

        if(scoreListView.notNil){
            scoreListView.remove;
            scoreListView = nil
        };
        usessionMouseEventsManager.remove;
        if(scoreList.size > 1) {
            this.makeScoreListView;
        };
		this.makeScoreView;
     }

     addtoScoreList{ |score|
        scoreList = scoreList.add(score);
        scoreEditorsList = scoreEditorsList.add(UScoreEditor(score));
        this.addCurrentScoreControllers;
        this.remake;
        this.changed(\activeScoreChanged);
     }

     goToHigherScore{ |i|
        scoreList = scoreList[..i];
        scoreEditorsList = scoreEditorsList[..i];
        this.addCurrentScoreControllers;
        this.changed(\activeScoreChanged);
        fork{ { this.remake; }.defer }
     }

     makeScoreListView{
        var listSize = scoreList.size;
        scoreListView = CompositeView(mainComposite,Rect(0,0,mainComposite.bounds.width,24));
        scoreListView.addFlowLayout;
        scoreList[..(listSize-2)].do{ |score,i|
            SmoothButton(scoreListView,60@16)
                .states_([[(i+1).asString++": "++score.name, Color.black, Color.clear]])
                .font_( font )
			    .border_(1).background_(Color.grey(0.8))
			    .radius_(5)
			    .canFocus_(false)
			    .action_({
                    this.goToHigherScore(i);
			    })
        };
        SmoothButton(scoreListView,16@16)
                .states_([[\up, Color.black, Color.clear]])
                .font_( font )
			    .border_(1).background_(Color.grey(0.8))
			    .radius_(5)
			    .canFocus_(false)
			    .action_({
                    UScoreEditorGUI( UScoreEditor( this.currentScore ) )
			    })

     }

     makeScoreView{
        var scoreEditor = scoreEditorsList.last;
        var score = scoreEditor.score;
        var scoreBounds = if(scoreList.size > 1) {
            mainComposite.bounds.copy.height_(mainComposite.bounds.height - 24).moveTo(0,24);
        }  {
            mainComposite.bounds.copy.moveTo(0,0)
        };

        if(scoreView.notNil) {
            scoreView.view.visible_(false);
            scoreView.view.focus(false);
            scoreView.remove;
        };

        numTracks = ((score.events.collect( _.track ).maxItem ? 10) + 2).max(12);

        scoreView = ScaledUserViewContainer(mainComposite,
        			scoreBounds,
        			Rect( 0, 0, score.duration.ceil.max(1), numTracks ),
        			5);

        //CONFIGURE scoreView
        scoreView.background = Color.gray(0.8);
        scoreView.composite.resize = 5;
	    scoreView.gridLines = [score.finiteDuration.ceil.max(1), numTracks];
		scoreView.gridMode = ['blocks','lines'];
		scoreView.sliderWidth = 8;
		scoreView.userView.view
		    .canReceiveDragHandler_({
                [ UChain, UScore ].includes(View.currentDrag.class) or: { 
	                View.currentDrag.class == Array && {
	                	View.currentDrag.flat.every({ |item| 
		                	[ UChain, UScore ].includes(item.class) 
	                	})
                	}
                }
            })
            .receiveDragHandler_({ |sink, x, y|
                View.currentDrag.postln;
                if( View.currentDrag.class == Array ) {
	               View.currentDrag.do({ |item|
		               this.currentScore.addEventToEmptyTrack(item.deepCopy);
	               });
                } {
                	this.currentScore.addEventToEmptyTrack(View.currentDrag.deepCopy);
                };
            })
            .beginDragAction_({
                this.selectedEvents;
            });

		//scoreView.maxZoom = [16,5];

		usessionMouseEventsManager = UScoreEditorGuiMouseEventsManager(this);

		scoreView
			.mouseDownAction_( { |v, x, y,mod,x2,y2, isInside, buttonNumber, clickCount| 	 // only drag when one event is selected for now

				var scaledPoint, shiftDown,altDown;

        		scaledPoint = [ x,y ].asPoint;
				shiftDown = ModKey( mod ).shift( \only );
				altDown = ModKey( mod ).alt( \only );

				usessionMouseEventsManager.mouseDownEvent(scaledPoint,Point(x2,y2),shiftDown,altDown,v,clickCount);

			} )
			.mouseMoveAction_( { |v, x, y, mod, x2, y2, isInside, buttonNumber|
				var snap = if(snapActive){snapH * v.gridSpacingH}{0};
				var shiftDown = ModKey( mod ).shift( \only );

				usessionMouseEventsManager.mouseMoveEvent(Point(x,y),Point(x2,y2),v,snap, shiftDown, v.fromBounds.width);

			} )
			.mouseUpAction_( { |v, x, y, mod, x2, y2, isInside, buttonNumber, clickCount|

				var shiftDown = ModKey( mod ).shift( \only );

				usessionMouseEventsManager.mouseUpEvent(Point(x,y),Point(x2,y2),shiftDown,v,isInside);

			} )
			.keyDownAction_( { |v, a,b,c|
				if( c == 127 ) {
					this.deleteSelected
				};
				if( c == 32 ) {
				    if(score.isStopped) {
				        score.prepareAndStart( ULib.servers, score.pos, true, score.loop);
				    } {
				        score.stop;
				    }

				}
			})
			.beforeDrawFunc_( {
			    var dur = score.finiteDuration.ceil.max(1);
				numTracks = ((score.events.collect( _.track ).maxItem ? ( numTracks - 2)) + 2)
					.max( numTracks );
				scoreView.fromBounds = Rect( 0, 0, dur, numTracks );
				scoreView.gridLines = [dur, numTracks];
				} )

			.unscaledDrawFunc_( { |v|
				var scPos, rect;
				rect = v.view.drawBounds.moveTo(0,0);

				//draw border
				GUI.pen.use({
					GUI.pen.addRect( rect.insetBy(0.5,0.5) );
					GUI.pen.fillColor = Color.gray(0.7).alpha_(0.5);
					GUI.pen.strokeColor = Color.gray(0.1).alpha_(0.5);
					GUI.pen.fill;
				});

				Pen.font = Font( Font.defaultSansFace, 10 );
				//draw events
				usessionMouseEventsManager.eventViews.do({ |eventView|
					eventView.draw(v, v.fromBounds.width );
				});

				//draw selection rectangle
				if(usessionMouseEventsManager.selectionRect.notNil) {
					Pen.color = Color.white;
					Pen.addRect(v.translateScale(usessionMouseEventsManager.selectionRect));
					Pen.stroke;
					Pen.color = Color.grey(0.3).alpha_(0.4);
					Pen.addRect(v.translateScale(usessionMouseEventsManager.selectionRect));
					Pen.fill;
				};

				//draw Transport line
				Pen.width = 2;
				Pen.color = Color.black.alpha_(0.5);
				scPos = v.translateScale( score.pos@0 );
				Pen.line( (scPos.x)@0, (scPos.x)@v.bounds.height);
				Pen.stroke;

				Pen.width = 1;
				Color.grey(0.5,1).set;
				Pen.strokeRect( rect.insetBy(0.5,0.5) );

		})
     }

     refresh{ scoreView.refresh; scoreView.refresh }
}