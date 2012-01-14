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

UEvent : UArchivable {
	
	classvar <>renderNumChannels = 2;
	classvar <>renderMaxTime = 60;

    var <startTime=0;
    var <>track=0;  //track number (horizontal segment) on the score editor
    var <duration = inf;
    var <>disabled = false;
    var <releaseSelf = true;

    /*
    If 'releaseSelf' is set to false, then uchains will not free themselves automatically when the events stop playing.
    If 'releaseSelf' is set to true, then uchains will free themselves even if the score is paused;
	*/
    
    // event duration of non-inf will cause the event to release its chain
    // instead of the chain releasing itself

	waitTime { this.subclassResponsibility(thisMethod) }
	prepareTime { ^startTime - this.waitTime } // time to start preparing
	
	<= { |that| ^this.prepareTime <= that.prepareTime } // sort support

    duration_{ this.subclassResponsibility(thisMethod) }
    isPausable_{ this.subclassResponsibility(thisMethod) }
    
    startTime_ { |newTime|
	   startTime = newTime; 
	   this.changed( \startTime )
    }

    endTime { ^startTime + this.duration; } // may be inf
    eventEndTime { ^startTime + this.eventSustain }

	disable { this.disabled_(true) }
	enable { this.disabled_(false) }
	toggleDisable { this.disabled_(disabled.not) }

    /*
    *   server: Server or Array[Server]
    */
    play { |server| // plays a single event plus waittime
        ("preparing "++this).postln;
        this.prepare(server);
        fork{
            this.waitTime.wait;
            ("playing "++this).postln;
            this.start(server);
            if( duration != inf ) {
	           this.eventSustain.wait;
	           this.release;
            };
        }
    }
    
    asScore { |duration, timeOffset=0|
	    
	    if( duration.isNil ) {
		    duration = this.finiteDuration;
	    };
	    
	    ^Score( 
	    		this.collectOSCBundles( UServerCenter.servers.first, timeOffset, duration  ) 
	    			++ [ [ duration, [ \c_set, 0,0 ] ] ]
	    	);
    }
    
    render { // standalone app friendly version
		arg path, maxTime=60, sampleRate = 44100,
			headerFormat = "AIFF", sampleFormat = "int24", options, inputFilePath, action;

		var file, oscFilePath, score, oldpgm;
		oldpgm = Score.program;
		Score.program = Server.program;
		oscFilePath = "/tmp/temp_oscscore" ++ UniqueID.next;
		score = this.asScore(maxTime);
		score.recordNRT(
			oscFilePath, path, inputFilePath, sampleRate, headerFormat, sampleFormat,
			options, "; rm" + oscFilePath, action: action;
		);
		Score.program = oldpgm;
    }
    
    writeAudioFile { |path, maxTime, action, headerFormat = "AIFF", sampleFormat = "int24"|
		var o;
		
		if( this.isFinite.not && { maxTime == nil } ) {
			maxTime = this.finiteDuration + 60;
		};
		
		o = ServerOptions.new
			.numOutputBusChannels_(renderNumChannels ? 2)
			.memSize_( 2**19 );
		path = path.replaceExtension( headerFormat.toLower );
		this.render( 
			path,
			maxTime, // explicit nil forces UScore to use score duration
			sampleRate: UServerCenter.servers.first.sampleRate,
			headerFormat: headerFormat, 
			sampleFormat: sampleFormat,
			options: o,
			action: action
		);		
    }

}